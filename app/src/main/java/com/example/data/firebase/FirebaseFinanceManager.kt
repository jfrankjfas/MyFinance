package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.entity.ArchivedPeriodEntity
import com.example.data.entity.BudgetEntity
import com.example.data.entity.ExtraordinaryFundEntity
import com.example.data.entity.ScheduledExpenseEntity
import com.example.data.entity.TransactionEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class FirebaseSyncStatus(
    val isConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val syncStatusText: String = "Conectando a Firebase...",
    val lastSyncTimestamp: Long? = null,
    val cloudTransactionsCount: Int = 0,
    val cloudBudgetsCount: Int = 0,
    val cloudScheduledCount: Int = 0,
    val userCloudId: String = ""
)

class FirebaseFinanceManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "FirebaseFinanceManager"
        private const val PREFS_NAME = "firebase_finance_prefs"
        private const val KEY_SEEDED = "has_initialized_data_v2"
    }

    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var activeUserId: String = "default_user"

    private val _syncStatus = MutableStateFlow(FirebaseSyncStatus())
    val syncStatus: StateFlow<FirebaseSyncStatus> = _syncStatus.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()

    // Callbacks for real-time synchronization from cloud to Room
    var onRemoteTransactionsReceived: ((List<TransactionEntity>) -> Unit)? = null
    var onRemoteBudgetsReceived: ((List<BudgetEntity>) -> Unit)? = null
    var onRemoteScheduledReceived: ((List<ScheduledExpenseEntity>) -> Unit)? = null
    var onRemoteFundsReceived: ((List<ExtraordinaryFundEntity>) -> Unit)? = null
    var onRemoteArchivedReceived: ((List<ArchivedPeriodEntity>) -> Unit)? = null

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:897107668731:android:2e6a77c96d1f6563a78a5e")
                    .setApiKey("AIzaSyBSYoTyNRGhcYiha3FYkPvUmDOlUDRqnpQ")
                    .setProjectId("gen-lang-client-0358975709")
                    .setStorageBucket("gen-lang-client-0358975709.firebasestorage.app")
                    .setGcmSenderId("897107668731")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }

            val db = FirebaseFirestore.getInstance()
            val cacheSettings = PersistentCacheSettings.newBuilder().build()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()
            db.firestoreSettings = settings
            firestore = db

            val firebaseAuth = FirebaseAuth.getInstance()
            auth = firebaseAuth

            // Sign in anonymously or use existing credentials to ensure security and real-time read/writes
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                activeUserId = currentUser.uid
                setupRealtimeListeners()
            } else {
                firebaseAuth.signInAnonymously()
                    .addOnSuccessListener { result ->
                        activeUserId = result.user?.uid ?: "user_${System.currentTimeMillis()}"
                        _syncStatus.value = _syncStatus.value.copy(
                            isConnected = true,
                            syncStatusText = "🟢 Firebase Online (Tiempo Real)",
                            userCloudId = activeUserId
                        )
                        setupRealtimeListeners()
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Anonymous sign-in failed, fallback to local identifier: ${e.message}")
                        activeUserId = getOrCreateLocalUserId()
                        _syncStatus.value = _syncStatus.value.copy(
                            isConnected = true,
                            syncStatusText = "🟢 Firebase Firestore Activo",
                            userCloudId = activeUserId
                        )
                        setupRealtimeListeners()
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isConnected = false,
                syncStatusText = "Modo Local (Firebase pendiente)"
            )
        }
    }

    private fun getOrCreateLocalUserId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var uid = prefs.getString("local_user_uid", null)
        if (uid == null) {
            uid = "fc_user_" + java.util.UUID.randomUUID().toString().take(12)
            prefs.edit().putString("local_user_uid", uid).apply()
        }
        return uid
    }

    fun updateUserAccount(email: String) {
        val cleanEmail = email.replace(".", "_").replace("@", "_at_")
        activeUserId = "acc_$cleanEmail"
        setupRealtimeListeners()
    }

    private fun setupRealtimeListeners() {
        val db = firestore ?: return
        clearListeners()

        val userDoc = db.collection("finanzas_users").document(activeUserId)

        // 1. Transactions Listener
        val txListener = userDoc.collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Transaction listen error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<TransactionEntity>()
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.getLong("id")?.toInt() ?: (doc.id.replace("tx_", "").toIntOrNull() ?: 0)
                            val title = doc.getString("title") ?: ""
                            val amount = doc.getDouble("amount") ?: 0.0
                            val category = doc.getString("category") ?: "Varios"
                            val type = doc.getString("type") ?: "EXPENSE"
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val note = doc.getString("note") ?: ""
                            val isAi = doc.getBoolean("isAiCategorized") ?: false
                            val attachment = doc.getString("attachmentUri")
                            val dueDate = doc.getLong("dueDate")
                            val hasReminder = doc.getBoolean("hasReminderScheduled") ?: false

                            if (title.isNotBlank()) {
                                list.add(
                                    TransactionEntity(
                                        id = id,
                                        title = title,
                                        amount = amount,
                                        category = category,
                                        type = type,
                                        timestamp = timestamp,
                                        note = note,
                                        isAiCategorized = isAi,
                                        attachmentUri = attachment,
                                        dueDate = dueDate,
                                        hasReminderScheduled = hasReminder
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing tx doc: ${e.message}")
                        }
                    }
                    _syncStatus.value = _syncStatus.value.copy(
                        isConnected = true,
                        cloudTransactionsCount = list.size,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        syncStatusText = "🟢 Firebase Online (Sincronizado)"
                    )
                    onRemoteTransactionsReceived?.invoke(list)
                }
            }
        activeListeners.add(txListener)

        // 2. Budgets Listener
        val budgetListener = userDoc.collection("budgets")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = mutableListOf<BudgetEntity>()
                    for (doc in snapshot.documents) {
                        val cat = doc.getString("category") ?: doc.id
                        val limit = doc.getDouble("limitAmount") ?: 0.0
                        val thresh = doc.getLong("alertThresholdPercent")?.toInt() ?: 80
                        list.add(BudgetEntity(category = cat, limitAmount = limit, alertThresholdPercent = thresh))
                    }
                    _syncStatus.value = _syncStatus.value.copy(cloudBudgetsCount = list.size)
                    onRemoteBudgetsReceived?.invoke(list)
                }
            }
        activeListeners.add(budgetListener)

        // 3. Scheduled Expenses Listener
        val scheduledListener = userDoc.collection("scheduled_expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = mutableListOf<ScheduledExpenseEntity>()
                    for (doc in snapshot.documents) {
                        val id = doc.getLong("id") ?: (doc.id.replace("se_", "").toLongOrNull() ?: 0L)
                        val title = doc.getString("title") ?: ""
                        val amount = doc.getDouble("amount") ?: 0.0
                        val category = doc.getString("category") ?: "Servicios"
                        val dueDate = doc.getLong("dueDate") ?: System.currentTimeMillis()
                        val isPaid = doc.getBoolean("isPaid") ?: false
                        val notify = doc.getBoolean("notifyReminder") ?: true
                        val attachment = doc.getString("attachmentUri")
                        val note = doc.getString("note") ?: ""
                        val isEmergency = doc.getBoolean("isEmergencyPriority") ?: false
                        list.add(
                            ScheduledExpenseEntity(
                                id = id,
                                title = title,
                                amount = amount,
                                category = category,
                                dueDate = dueDate,
                                isPaid = isPaid,
                                notifyReminder = notify,
                                attachmentUri = attachment,
                                note = note,
                                isEmergencyPriority = isEmergency
                            )
                        )
                    }
                    _syncStatus.value = _syncStatus.value.copy(cloudScheduledCount = list.size)
                    onRemoteScheduledReceived?.invoke(list)
                }
            }
        activeListeners.add(scheduledListener)

        // 4. Extraordinary Funds Listener
        val fundsListener = userDoc.collection("extraordinary_funds")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = mutableListOf<ExtraordinaryFundEntity>()
                    for (doc in snapshot.documents) {
                        val id = doc.getLong("id") ?: (doc.id.replace("ef_", "").toLongOrNull() ?: 0L)
                        val title = doc.getString("title") ?: ""
                        val total = doc.getDouble("totalAmount") ?: 0.0
                        val symbol = doc.getString("currencySymbol") ?: "$"
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        val note = doc.getString("note") ?: ""
                        val allocJson = doc.getString("allocationsJson") ?: "[]"
                        list.add(
                            ExtraordinaryFundEntity(
                                id = id,
                                title = title,
                                totalAmount = total,
                                currencySymbol = symbol,
                                createdAt = createdAt,
                                note = note,
                                allocationsJson = allocJson
                            )
                        )
                    }
                    onRemoteFundsReceived?.invoke(list)
                }
            }
        activeListeners.add(fundsListener)

        // 5. Archived Periods Listener
        val archiveListener = userDoc.collection("archived_periods")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = mutableListOf<ArchivedPeriodEntity>()
                    for (doc in snapshot.documents) {
                        val id = doc.getLong("id") ?: (doc.id.replace("ap_", "").toLongOrNull() ?: 0L)
                        val title = doc.getString("title") ?: ""
                        val mode = doc.getString("periodMode") ?: "MONTHLY"
                        val archivedAt = doc.getLong("archivedAt") ?: System.currentTimeMillis()
                        val limit = doc.getDouble("budgetLimit") ?: 0.0
                        val spent = doc.getDouble("totalSpent") ?: 0.0
                        val sched = doc.getDouble("totalScheduled") ?: 0.0
                        val symbol = doc.getString("currencySymbol") ?: "$"
                        val note = doc.getString("note") ?: ""
                        val txsJson = doc.getString("transactionsJson") ?: "[]"
                        val schJson = doc.getString("scheduledJson") ?: "[]"
                        val isClosed = doc.getBoolean("isClosed") ?: false
                        list.add(
                            ArchivedPeriodEntity(
                                id = id,
                                title = title,
                                periodMode = mode,
                                archivedAt = archivedAt,
                                budgetLimit = limit,
                                totalSpent = spent,
                                totalScheduled = sched,
                                currencySymbol = symbol,
                                note = note,
                                transactionsJson = txsJson,
                                scheduledJson = schJson,
                                isClosed = isClosed
                            )
                        )
                    }
                    onRemoteArchivedReceived?.invoke(list)
                }
            }
        activeListeners.add(archiveListener)
    }

    private fun clearListeners() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
    }

    // --- WRITE OPERATIONS TO FIRESTORE (Real-time Cloud Sync) ---

    fun saveTransactionToCloud(tx: TransactionEntity) {
        val db = firestore ?: return
        val docId = if (tx.id > 0) "tx_${tx.id}" else "tx_${tx.timestamp}_${tx.title.hashCode()}"
        val data = hashMapOf(
            "id" to tx.id,
            "title" to tx.title,
            "amount" to tx.amount,
            "category" to tx.category,
            "type" to tx.type,
            "timestamp" to tx.timestamp,
            "note" to tx.note,
            "isAiCategorized" to tx.isAiCategorized,
            "attachmentUri" to (tx.attachmentUri ?: ""),
            "dueDate" to (tx.dueDate ?: 0L),
            "hasReminderScheduled" to tx.hasReminderScheduled,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("finanzas_users").document(activeUserId)
            .collection("transactions").document(docId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                _syncStatus.value = _syncStatus.value.copy(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    syncStatusText = "🟢 Guardado en Firebase en vivo"
                )
            }
    }

    fun deleteTransactionFromCloud(tx: TransactionEntity) {
        val db = firestore ?: return
        val docId = "tx_${tx.id}"
        db.collection("finanzas_users").document(activeUserId)
            .collection("transactions").document(docId)
            .delete()
    }

    fun saveBudgetToCloud(b: BudgetEntity) {
        val db = firestore ?: return
        val docId = "bg_${b.category.replace("/", "_")}"
        val data = hashMapOf(
            "category" to b.category,
            "limitAmount" to b.limitAmount,
            "alertThresholdPercent" to b.alertThresholdPercent,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("finanzas_users").document(activeUserId)
            .collection("budgets").document(docId)
            .set(data, SetOptions.merge())
    }

    fun saveScheduledExpenseToCloud(s: ScheduledExpenseEntity) {
        val db = firestore ?: return
        val docId = if (s.id > 0) "se_${s.id}" else "se_${s.dueDate}_${s.title.hashCode()}"
        val data = hashMapOf(
            "id" to s.id,
            "title" to s.title,
            "amount" to s.amount,
            "category" to s.category,
            "dueDate" to s.dueDate,
            "isPaid" to s.isPaid,
            "notifyReminder" to s.notifyReminder,
            "attachmentUri" to (s.attachmentUri ?: ""),
            "note" to s.note,
            "isEmergencyPriority" to s.isEmergencyPriority,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("finanzas_users").document(activeUserId)
            .collection("scheduled_expenses").document(docId)
            .set(data, SetOptions.merge())
    }

    fun deleteScheduledExpenseFromCloud(s: ScheduledExpenseEntity) {
        val db = firestore ?: return
        val docId = "se_${s.id}"
        db.collection("finanzas_users").document(activeUserId)
            .collection("scheduled_expenses").document(docId)
            .delete()
    }

    // Wipes all user collections in Firestore to start 100% completely from zero
    suspend fun clearAllUserDataFromCloud() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext
        try {
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                syncStatusText = "Limpiando datos en la nube..."
            )
            val userDoc = db.collection("finanzas_users").document(activeUserId)
            val subcollections = listOf("transactions", "budgets", "scheduled_expenses", "extraordinary_funds", "archived_periods")
            for (colName in subcollections) {
                val snapshot = userDoc.collection(colName).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            }
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                cloudTransactionsCount = 0,
                cloudBudgetsCount = 0,
                cloudScheduledCount = 0,
                lastSyncTimestamp = System.currentTimeMillis(),
                syncStatusText = "🟢 Firebase: Base de datos limpia (Desde Cero)"
            )
            Log.d(TAG, "Successfully wiped all cloud collections for user $activeUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cloud data: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                syncStatusText = "Error al limpiar la nube"
            )
        }
    }

    fun saveExtraordinaryFundToCloud(f: ExtraordinaryFundEntity) {
        val db = firestore ?: return
        val docId = if (f.id > 0) "ef_${f.id}" else "ef_${f.createdAt}_${f.title.hashCode()}"
        val data = hashMapOf(
            "id" to f.id,
            "title" to f.title,
            "totalAmount" to f.totalAmount,
            "currencySymbol" to f.currencySymbol,
            "createdAt" to f.createdAt,
            "note" to f.note,
            "allocationsJson" to f.allocationsJson,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("finanzas_users").document(activeUserId)
            .collection("extraordinary_funds").document(docId)
            .set(data, SetOptions.merge())
    }

    fun deleteExtraordinaryFundFromCloud(f: ExtraordinaryFundEntity) {
        val db = firestore ?: return
        val docId = "ef_${f.id}"
        db.collection("finanzas_users").document(activeUserId)
            .collection("extraordinary_funds").document(docId)
            .delete()
    }

    fun saveArchivedPeriodToCloud(ap: ArchivedPeriodEntity) {
        val db = firestore ?: return
        val docId = if (ap.id > 0) "ap_${ap.id}" else "ap_${ap.archivedAt}_${ap.title.hashCode()}"
        val data = hashMapOf(
            "id" to ap.id,
            "title" to ap.title,
            "periodMode" to ap.periodMode,
            "archivedAt" to ap.archivedAt,
            "budgetLimit" to ap.budgetLimit,
            "totalSpent" to ap.totalSpent,
            "totalScheduled" to ap.totalScheduled,
            "currencySymbol" to ap.currencySymbol,
            "note" to ap.note,
            "transactionsJson" to ap.transactionsJson,
            "scheduledJson" to ap.scheduledJson,
            "isClosed" to ap.isClosed,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("finanzas_users").document(activeUserId)
            .collection("archived_periods").document(docId)
            .set(data, SetOptions.merge())
    }

    fun deleteArchivedPeriodFromCloud(ap: ArchivedPeriodEntity) {
        val db = firestore ?: return
        val docId = "ap_${ap.id}"
        db.collection("finanzas_users").document(activeUserId)
            .collection("archived_periods").document(docId)
            .delete()
    }

    // Upload entire local dataset to Firebase Firestore
    suspend fun syncAllLocalToCloud(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        scheduled: List<ScheduledExpenseEntity>,
        funds: List<ExtraordinaryFundEntity>,
        archived: List<ArchivedPeriodEntity>
    ) {
        _syncStatus.value = _syncStatus.value.copy(
            isSyncing = true,
            syncStatusText = "Sincronizando con Firebase..."
        )
        try {
            transactions.forEach { saveTransactionToCloud(it) }
            budgets.forEach { saveBudgetToCloud(it) }
            scheduled.forEach { saveScheduledExpenseToCloud(it) }
            funds.forEach { saveExtraordinaryFundToCloud(it) }
            archived.forEach { saveArchivedPeriodToCloud(it) }

            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTimestamp = System.currentTimeMillis(),
                syncStatusText = "🟢 Firebase Online (Sincronizado completo)"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to cloud: ${e.message}")
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                syncStatusText = "Error al sincronizar con Firebase"
            )
        }
    }
}
