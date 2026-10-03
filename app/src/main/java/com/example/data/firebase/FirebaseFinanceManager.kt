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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

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

data class CloudDataSnapshot(
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val scheduledExpenses: List<ScheduledExpenseEntity> = emptyList(),
    val extraordinaryFunds: List<ExtraordinaryFundEntity> = emptyList(),
    val archivedPeriods: List<ArchivedPeriodEntity> = emptyList(),
    val userPin: String? = null
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

            // Proactively update latestVersion metadata to 2.1 in Firestore
            publishVersionToCloud(
                version = "2.1",
                releaseNotes = "Versión 2.1: Continuación automática de presupuesto entre quincenas al superar el 100%, selección de origen de fondos (Presupuesto o Fondos Extraordinarios) en Gastos Programados, soporte completo para pagos recurrentes mensuales con renovación automática y corrección en la edición/eliminación de registros e inversiones.",
                apkDownloadUrl = "https://github.com/jfrankjfas/MyFinance/releases/download/v2.1/FinanzasClaras-v2.1.apk"
            )

            val firebaseAuth = FirebaseAuth.getInstance()
            auth = firebaseAuth

            // Sign in anonymously or use existing credentials to ensure security and real-time read/writes
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                if (activeUserId == "default_user") {
                    activeUserId = currentUser.uid
                    setupRealtimeListeners()
                }
            } else {
                firebaseAuth.signInAnonymously()
                    .addOnSuccessListener { result ->
                        if (activeUserId == "default_user") {
                            activeUserId = result.user?.uid ?: "user_${System.currentTimeMillis()}"
                            _syncStatus.value = _syncStatus.value.copy(
                                isConnected = true,
                                syncStatusText = "🟢 Firebase Online (Tiempo Real)",
                                userCloudId = activeUserId
                            )
                            setupRealtimeListeners()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Anonymous sign-in failed, fallback to local identifier: ${e.message}")
                        if (activeUserId == "default_user") {
                            activeUserId = getOrCreateLocalUserId()
                            _syncStatus.value = _syncStatus.value.copy(
                                isConnected = true,
                                syncStatusText = "🟢 Firebase Firestore Activo",
                                userCloudId = activeUserId
                            )
                            setupRealtimeListeners()
                        }
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
        if (email.isBlank()) {
            disconnectUser()
            return
        }
        val cleanEmail = email.trim().lowercase().removePrefix("acc_").replace(".", "_").replace("@", "_at_")
        activeUserId = "acc_$cleanEmail"
        _syncStatus.value = _syncStatus.value.copy(
            isConnected = true,
            syncStatusText = "🟢 Conectado con Gmail ($email)",
            userCloudId = activeUserId
        )
        setupRealtimeListeners()
    }

    fun disconnectUser() {
        clearListeners()
        activeUserId = "unauthenticated"
        _syncStatus.value = _syncStatus.value.copy(
            isConnected = false,
            syncStatusText = "Sesión cerrada (Registros protegidos en nube)",
            userCloudId = "",
            cloudTransactionsCount = 0,
            cloudBudgetsCount = 0,
            cloudScheduledCount = 0
        )
    }

    private fun parseTransactionsFromDocs(docs: List<com.google.firebase.firestore.DocumentSnapshot>): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        for (doc in docs) {
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
                val rawAmount = doc.getDouble("amount") ?: 0.0
                val rawOrigAmount = doc.getDouble("originalAmount") ?: 0.0
                val explicitCurr = doc.getString("originalCurrency")?.uppercase()?.trim()

                // Check for explicit [X USD] tag generated in note
                val usdMatch = Regex("""\[(\d+(\.\d+)?) USD\]""").find(note)

                val isRealUsd = when {
                    explicitCurr == "USD" && rawOrigAmount > 0.0 && rawAmount > rawOrigAmount * 20.0 -> true
                    usdMatch != null -> true
                    explicitCurr == "USD" && rawOrigAmount > 0.0 && rawOrigAmount != rawAmount -> true
                    else -> false
                }

                val finalOrigCurr = if (isRealUsd) "USD" else "NIO"
                val finalExRate = if (isRealUsd) (doc.getDouble("exchangeRate") ?: 36.6243) else 1.0

                val finalBaseAmount: Double
                val finalOrigAmount: Double

                if (isRealUsd) {
                    val usdVal = usdMatch?.groupValues?.get(1)?.toDoubleOrNull()
                        ?: if (rawOrigAmount > 0.0 && rawOrigAmount != rawAmount) rawOrigAmount
                        else (rawAmount / finalExRate)

                    finalOrigAmount = usdVal
                    finalBaseAmount = if (rawAmount >= usdVal * 20.0) rawAmount else (usdVal * finalExRate)
                } else {
                    // Transaction is natively in Córdobas NIO
                    finalBaseAmount = rawAmount
                    finalOrigAmount = rawAmount
                }

                if (title.isNotBlank()) {
                    list.add(
                        TransactionEntity(
                            id = id,
                            title = title,
                            amount = finalBaseAmount,
                            category = category,
                            type = type,
                            timestamp = timestamp,
                            note = note,
                            isAiCategorized = isAi,
                            attachmentUri = attachment,
                            dueDate = dueDate,
                            hasReminderScheduled = hasReminder,
                            originalAmount = finalOrigAmount,
                            originalCurrency = finalOrigCurr,
                            exchangeRate = finalExRate
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing tx doc: ${e.message}")
            }
        }
        return list
    }

    private fun parseBudgetsFromDocs(docs: List<com.google.firebase.firestore.DocumentSnapshot>): List<BudgetEntity> {
        val list = mutableListOf<BudgetEntity>()
        for (doc in docs) {
            try {
                val cat = doc.getString("category") ?: doc.id.replace("bg_", "").replace("_", "/")
                val limit = doc.getDouble("limitAmount") ?: 0.0
                val thresh = doc.getLong("alertThresholdPercent")?.toInt() ?: 80
                if (cat.isNotBlank()) {
                    list.add(BudgetEntity(category = cat, limitAmount = limit, alertThresholdPercent = thresh))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing budget doc: ${e.message}")
            }
        }
        return list
    }

    private fun parseScheduledFromDocs(docs: List<com.google.firebase.firestore.DocumentSnapshot>): List<ScheduledExpenseEntity> {
        val list = mutableListOf<ScheduledExpenseEntity>()
        for (doc in docs) {
            try {
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
                val isRecurring = doc.getBoolean("isRecurringMonthly") ?: false
                val fundingSource = doc.getString("fundingSource") ?: "PERIOD_BUDGET"
                val fundId = doc.getLong("extraordinaryFundId")
                val fundTitle = doc.getString("extraordinaryFundTitle") ?: ""

                if (title.isNotBlank()) {
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
                            isEmergencyPriority = isEmergency,
                            isRecurringMonthly = isRecurring,
                            fundingSource = fundingSource,
                            extraordinaryFundId = fundId,
                            extraordinaryFundTitle = fundTitle
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing scheduled doc: ${e.message}")
            }
        }
        return list
    }

    private fun parseFundsFromDocs(docs: List<com.google.firebase.firestore.DocumentSnapshot>): List<ExtraordinaryFundEntity> {
        val list = mutableListOf<ExtraordinaryFundEntity>()
        for (doc in docs) {
            try {
                val id = doc.getLong("id") ?: (doc.id.replace("ef_", "").toLongOrNull() ?: 0L)
                val title = doc.getString("title") ?: ""
                val total = doc.getDouble("totalAmount") ?: 0.0
                val symbol = doc.getString("currencySymbol") ?: "$"
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val note = doc.getString("note") ?: ""
                val allocJson = doc.getString("allocationsJson") ?: "[]"

                if (title.isNotBlank()) {
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
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing fund doc: ${e.message}")
            }
        }
        return list
    }

    private fun parseArchivedFromDocs(docs: List<com.google.firebase.firestore.DocumentSnapshot>): List<ArchivedPeriodEntity> {
        val list = mutableListOf<ArchivedPeriodEntity>()
        for (doc in docs) {
            try {
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

                if (title.isNotBlank()) {
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
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing archive doc: ${e.message}")
            }
        }
        return list
    }

    suspend fun fetchUserDataFromCloud(email: String): CloudDataSnapshot = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext CloudDataSnapshot()
        if (email.isBlank()) return@withContext CloudDataSnapshot()

        val rawEmail = email.trim().lowercase()
        val cleanEmail = rawEmail.removePrefix("acc_").replace(".", "_").replace("@", "_at_")
        val primaryId = "acc_$cleanEmail"
        activeUserId = primaryId

        val txMap = LinkedHashMap<String, TransactionEntity>()
        val bgMap = LinkedHashMap<String, BudgetEntity>()
        val scMap = LinkedHashMap<String, ScheduledExpenseEntity>()
        val efMap = LinkedHashMap<String, ExtraordinaryFundEntity>()
        val apMap = LinkedHashMap<String, ArchivedPeriodEntity>()
        var cloudUserPin: String? = null

        val candidateIds = listOf(primaryId, cleanEmail)

        for (cand in candidateIds) {
            try {
                val userDoc = db.collection("finanzas_users").document(cand)

                kotlinx.coroutines.withTimeoutOrNull(3500L) {
                    kotlinx.coroutines.coroutineScope {
                        val pinDeferred = async {
                            try {
                                val snap = userDoc.get().await()
                                snap.getString("pinuser") ?: snap.getString("securityPin")
                            } catch (e: Exception) { null }
                        }
                        val txDeferred = async {
                            try { userDoc.collection("transactions").get().await().documents }
                            catch (e: Exception) { emptyList() }
                        }
                        val bgDeferred = async {
                            try { userDoc.collection("budgets").get().await().documents }
                            catch (e: Exception) { emptyList() }
                        }
                        val scDeferred = async {
                            try { userDoc.collection("scheduled_expenses").get().await().documents }
                            catch (e: Exception) { emptyList() }
                        }
                        val efDeferred = async {
                            try { userDoc.collection("extraordinary_funds").get().await().documents }
                            catch (e: Exception) { emptyList() }
                        }
                        val apDeferred = async {
                            try { userDoc.collection("archived_periods").get().await().documents }
                            catch (e: Exception) { emptyList() }
                        }

                        val pin = pinDeferred.await()
                        if (cloudUserPin.isNullOrBlank() && !pin.isNullOrBlank()) {
                            cloudUserPin = pin
                        }

                        val txDocs = txDeferred.await()
                        parseTransactionsFromDocs(txDocs).forEach { tx ->
                            val key = if (tx.id > 0) "id_${tx.id}" else "${tx.title}_${tx.timestamp}_${tx.amount}"
                            txMap[key] = tx
                        }

                        val bgDocs = bgDeferred.await()
                        parseBudgetsFromDocs(bgDocs).forEach { bg ->
                            bgMap[bg.category.lowercase().trim()] = bg
                        }

                        val scDocs = scDeferred.await()
                        parseScheduledFromDocs(scDocs).forEach { sc ->
                            val key = if (sc.id > 0) "id_${sc.id}" else "${sc.title}_${sc.dueDate}"
                            scMap[key] = sc
                        }

                        val efDocs = efDeferred.await()
                        parseFundsFromDocs(efDocs).forEach { ef ->
                            val key = if (ef.id > 0) "id_${ef.id}" else ef.title.trim()
                            efMap[key] = ef
                        }

                        val apDocs = apDeferred.await()
                        parseArchivedFromDocs(apDocs).forEach { ap ->
                            val key = if (ap.id > 0) "id_${ap.id}" else "${ap.title}_${ap.archivedAt}"
                            apMap[key] = ap
                        }
                    }
                }

                if (txMap.isNotEmpty() || bgMap.isNotEmpty()) {
                    break
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error fetching from $cand: ${e.message}")
            }
        }

        // Also check standalone 'pinuser' collection with timeout
        if (cloudUserPin.isNullOrBlank()) {
            try {
                kotlinx.coroutines.withTimeoutOrNull(2000L) {
                    val pinSnap = db.collection("pinuser").document(cleanEmail).get().await()
                    cloudUserPin = pinSnap.getString("pinuser") ?: pinSnap.getString("pin") ?: pinSnap.getString("securityPin")
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        val txList = txMap.values.toList()
        val bgList = bgMap.values.toList()
        val scList = scMap.values.toList()
        val efList = efMap.values.toList()
        val apList = apMap.values.toList()

        _syncStatus.value = _syncStatus.value.copy(
            isConnected = true,
            cloudTransactionsCount = txList.size,
            cloudBudgetsCount = bgList.size,
            cloudScheduledCount = scList.size,
            lastSyncTimestamp = System.currentTimeMillis(),
            syncStatusText = "🟢 Firebase Online (${txList.size} movs)"
        )

        CloudDataSnapshot(
            transactions = txList,
            budgets = bgList,
            scheduledExpenses = scList,
            extraordinaryFunds = efList,
            archivedPeriods = apList,
            userPin = cloudUserPin
        )
    }

    private val syncMutex = Mutex()

    suspend fun syncAllBidirectional(
        email: String,
        localTxs: List<TransactionEntity>,
        localBudgets: List<BudgetEntity>,
        localScheduled: List<ScheduledExpenseEntity>,
        localFunds: List<ExtraordinaryFundEntity>,
        localArchived: List<ArchivedPeriodEntity>
    ): CloudDataSnapshot = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val rawEmail = email.trim().lowercase()
        val cleanEmail = rawEmail.removePrefix("acc_").replace(".", "_").replace("@", "_at_")
        val primaryId = "acc_$cleanEmail"
        activeUserId = primaryId

        if (!syncMutex.tryLock()) {
            Log.d(TAG, "Sync already running in parallel, returning cloud snapshot")
            return@withContext fetchUserDataFromCloud(email)
        }

        _syncStatus.value = _syncStatus.value.copy(
            isSyncing = true,
            syncStatusText = "Sincronizando con Firebase..."
        )

        try {
            // 1. Pull everything from cloud across all candidate locations
            val cloudSnapshot = fetchUserDataFromCloud(email)

            val db = firestore
            if (db != null) {
                val userDocRef = db.collection("finanzas_users").document(primaryId)
                val userMeta = hashMapOf(
                    "email" to rawEmail,
                    "lastSync" to System.currentTimeMillis(),
                    "userId" to primaryId,
                    "updatedAt" to System.currentTimeMillis()
                )
                try {
                    withTimeoutOrNull(3000L) {
                        userDocRef.set(userMeta, SetOptions.merge()).await()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "User doc metadata update warning: ${e.message}")
                }
            }

            // Combine into unified maps
            val mergedTxs = (cloudSnapshot.transactions + localTxs).distinctBy {
                if (it.id > 0) "id_${it.id}" else "${it.title}_${it.timestamp}_${it.amount}"
            }
            val mergedBudgets = (cloudSnapshot.budgets + localBudgets).distinctBy { it.category.lowercase().trim() }
            val mergedScheduled = (cloudSnapshot.scheduledExpenses + localScheduled).distinctBy {
                if (it.id > 0) "id_${it.id}" else "${it.title}_${it.dueDate}"
            }
            val mergedFunds = (cloudSnapshot.extraordinaryFunds + localFunds).distinctBy {
                if (it.id > 0) "id_${it.id}" else it.title.trim()
            }
            val mergedArchived = (cloudSnapshot.archivedPeriods + localArchived).distinctBy {
                if (it.id > 0) "id_${it.id}" else "${it.title}_${it.archivedAt}"
            }

            // Only push to cloud if there is local data that needs merging
            val hasLocalData = localTxs.isNotEmpty() || localBudgets.isNotEmpty() ||
                    localScheduled.isNotEmpty() || localFunds.isNotEmpty() || localArchived.isNotEmpty()
            if (hasLocalData) {
                syncAllLocalToCloud(mergedTxs, mergedBudgets, mergedScheduled, mergedFunds, mergedArchived)
            }

            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                isConnected = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                cloudTransactionsCount = mergedTxs.size,
                cloudBudgetsCount = mergedBudgets.size,
                cloudScheduledCount = mergedScheduled.size,
                syncStatusText = "🟢 Firebase Online (${mergedTxs.size} movs sincronizados)"
            )

            CloudDataSnapshot(
                transactions = mergedTxs,
                budgets = mergedBudgets,
                scheduledExpenses = mergedScheduled,
                extraordinaryFunds = mergedFunds,
                archivedPeriods = mergedArchived,
                userPin = cloudSnapshot.userPin
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in bidirectional sync: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                syncStatusText = "🟢 Firebase Listo (Sincronizado local)"
            )
            fetchUserDataFromCloud(email)
        } finally {
            _syncStatus.value = _syncStatus.value.copy(isSyncing = false)
            syncMutex.unlock()
        }
    }

    /**
     * Guarda el PIN de seguridad vinculado a la cuenta Gmail del usuario en Firebase Firestore.
     * Se persiste tanto en el documento del usuario en 'finanzas_users' como en la colección dedicada 'pinuser'.
     */
    fun saveUserPin(email: String, pin: String) {
        if (email.isBlank() || pin.isBlank()) return
        val db = firestore ?: return
        val cleanEmail = email.trim().lowercase().replace(".", "_").replace("@", "_at_")
        val userId = "acc_$cleanEmail"

        val pinData = mapOf(
            "pinuser" to pin,
            "securityPin" to pin,
            "email" to email.trim().lowercase(),
            "updatedAt" to System.currentTimeMillis()
        )

        // 1. Guardar en documento del usuario
        db.collection("finanzas_users").document(userId)
            .set(pinData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "PIN guardado exitosamente en finanzas_users/$userId")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error guardando PIN en finanzas_users: ${e.message}")
            }

        // 2. Guardar en colección 'pinuser' explícita
        db.collection("pinuser").document(cleanEmail)
            .set(pinData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "PIN guardado exitosamente en colección pinuser/$cleanEmail")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error guardando PIN en colección pinuser: ${e.message}")
            }
    }

    /**
     * Recupera el PIN de seguridad almacenado en la nube para la cuenta Gmail del usuario.
     */
    suspend fun fetchUserPin(email: String): String? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext null
        if (email.isBlank()) return@withContext null
        val cleanEmail = email.trim().lowercase().replace(".", "_").replace("@", "_at_")
        val userId = "acc_$cleanEmail"

        try {
            // Intentar documento de usuario
            val userSnap = db.collection("finanzas_users").document(userId).get().await()
            val pin = userSnap.getString("pinuser") ?: userSnap.getString("securityPin")
            if (!pin.isNullOrBlank()) return@withContext pin

            // Intentar colección 'pinuser'
            val pinSnap = db.collection("pinuser").document(cleanEmail).get().await()
            return@withContext pinSnap.getString("pinuser") ?: pinSnap.getString("pin") ?: pinSnap.getString("securityPin")
        } catch (e: Exception) {
            Log.w(TAG, "Error obteniendo PIN de la nube: ${e.message}")
            null
        }
    }

    /**
     * Publica metadatos de versión en Firestore (app_config/version) para que cualquier
     * dispositivo con la app instalada detecte la nueva versión v1.2 de inmediato.
     */
    fun publishVersionToCloud(
        version: String = "2.1",
        releaseNotes: String = "Versión 2.1: Continuación automática de presupuesto entre quincenas al superar el 100%, selección de origen de fondos (Presupuesto o Fondos Extraordinarios) en Gastos Programados, soporte completo para pagos recurrentes mensuales con renovación automática y corrección en la edición/eliminación de registros e inversiones.",
        apkDownloadUrl: String = "https://github.com/jfrankjfas/MyFinance/releases/download/v2.1/FinanzasClaras-v2.1.apk"
    ) {
        val db = firestore ?: return
        val data = hashMapOf(
            "latestVersion" to version,
            "releaseNotes" to releaseNotes,
            "apkDownloadUrl" to apkDownloadUrl,
            "releaseDate" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("app_config").document("version")
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Versión v$version publicada exitosamente en Firestore app_config/version")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error publicando versión en Firestore: ${e.message}")
            }
    }

    private fun setupRealtimeListeners() {
        val db = firestore ?: return
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) {
            clearListeners()
            return
        }
        clearListeners()

        val userDoc = db.collection("finanzas_users").document(activeUserId)

        // 1. Transactions Listener
        val txListener = userDoc.collection("transactions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Transaction listen error: ${error.message}")
                    val msg = if (error.message?.contains("does not exist", ignoreCase = true) == true ||
                        error.message?.contains("NOT_FOUND", ignoreCase = true) == true) {
                        "⚠️ Base de datos Firestore no creada en Firebase Console"
                    } else if (error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                        "⚠️ Permisos pendientes en reglas de Firestore"
                    } else {
                        "Modo Local (Firebase: ${error.code})"
                    }
                    _syncStatus.value = _syncStatus.value.copy(
                        isConnected = false,
                        syncStatusText = msg
                    )
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = parseTransactionsFromDocs(snapshot.documents)
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
                        val isRecurring = doc.getBoolean("isRecurringMonthly") ?: false
                        val fundingSource = doc.getString("fundingSource") ?: "PERIOD_BUDGET"
                        val fundId = doc.getLong("extraordinaryFundId")
                        val fundTitle = doc.getString("extraordinaryFundTitle") ?: ""
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
                                isEmergencyPriority = isEmergency,
                                isRecurringMonthly = isRecurring,
                                fundingSource = fundingSource,
                                extraordinaryFundId = fundId,
                                extraordinaryFundTitle = fundTitle
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
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) return
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
            "originalAmount" to tx.originalAmount,
            "originalCurrency" to tx.originalCurrency,
            "exchangeRate" to tx.exchangeRate,
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
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) return
        val db = firestore ?: return
        val userCol = db.collection("finanzas_users").document(activeUserId).collection("transactions")
        if (tx.id > 0) {
            userCol.document("tx_${tx.id}").delete()
        }
        userCol.document("tx_${tx.timestamp}_${tx.title.hashCode()}").delete()
        // Also ensure any lingering transaction with the same timestamp and title is removed
        userCol.whereEqualTo("timestamp", tx.timestamp).whereEqualTo("title", tx.title).get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { it.reference.delete() }
            }
    }

    fun saveBudgetToCloud(b: BudgetEntity) {
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) return
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

    fun deleteBudgetFromCloud(category: String) {
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) return
        val db = firestore ?: return
        val userCol = db.collection("finanzas_users").document(activeUserId).collection("budgets")
        val docId = "bg_${category.replace("/", "_")}"
        userCol.document(docId).delete()
        val clean = category.replace("ó", "o").replace("Ó", "O").replace("/", "_")
        userCol.document("bg_$clean").delete()
        userCol.document("bg_${category.lowercase()}").delete()
        userCol.whereEqualTo("category", category).get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { it.reference.delete() }
            }
    }

    fun saveScheduledExpenseToCloud(s: ScheduledExpenseEntity) {
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) return
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
            "isRecurringMonthly" to s.isRecurringMonthly,
            "fundingSource" to s.fundingSource,
            "extraordinaryFundId" to (s.extraordinaryFundId ?: 0L),
            "extraordinaryFundTitle" to s.extraordinaryFundTitle,
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("finanzas_users").document(activeUserId)
            .collection("scheduled_expenses").document(docId)
            .set(data, SetOptions.merge())
    }

    fun deleteScheduledExpenseFromCloud(s: ScheduledExpenseEntity) {
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) return
        val db = firestore ?: return
        val userCol = db.collection("finanzas_users").document(activeUserId).collection("scheduled_expenses")
        if (s.id > 0) userCol.document("se_${s.id}").delete()
        userCol.document("se_${s.dueDate}_${s.title.hashCode()}").delete()
        userCol.whereEqualTo("title", s.title).get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { it.reference.delete() }
            }
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
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) return
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
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) return
        val db = firestore ?: return
        val docId = "ef_${f.id}"
        db.collection("finanzas_users").document(activeUserId)
            .collection("extraordinary_funds").document(docId)
            .delete()
    }

    fun saveArchivedPeriodToCloud(ap: ArchivedPeriodEntity) {
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) return
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
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) return
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
    ) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (activeUserId == "unauthenticated" || activeUserId.isBlank()) return@withContext
        val db = firestore ?: return@withContext
        _syncStatus.value = _syncStatus.value.copy(
            isSyncing = true,
            syncStatusText = "Sincronizando con Firebase..."
        )
        try {
            val userDoc = db.collection("finanzas_users").document(activeUserId)
            try {
                withTimeoutOrNull(3000L) {
                    userDoc.set(mapOf("lastBackupAt" to System.currentTimeMillis()), SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                Log.w(TAG, "lastBackupAt warning: ${e.message}")
            }

            // Transactions
            transactions.chunked(300).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { tx ->
                    val docId = if (tx.id > 0) "tx_${tx.id}" else "tx_${tx.timestamp}_${tx.title.hashCode()}"
                    val ref = userDoc.collection("transactions").document(docId)
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
                        "originalAmount" to tx.originalAmount,
                        "originalCurrency" to tx.originalCurrency,
                        "exchangeRate" to tx.exchangeRate,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(ref, data, SetOptions.merge())
                }
                try {
                    withTimeoutOrNull(4000L) { batch.commit().await() }
                } catch (e: Exception) {
                    Log.w(TAG, "Transaction batch warning: ${e.message}")
                }
            }

            // Budgets
            if (budgets.isNotEmpty()) {
                val batch = db.batch()
                budgets.forEach { b ->
                    val docId = "bg_${b.category.replace("/", "_")}"
                    val ref = userDoc.collection("budgets").document(docId)
                    val data = hashMapOf(
                        "category" to b.category,
                        "limitAmount" to b.limitAmount,
                        "alertThresholdPercent" to b.alertThresholdPercent,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(ref, data, SetOptions.merge())
                }
                try {
                    withTimeoutOrNull(4000L) { batch.commit().await() }
                } catch (e: Exception) {
                    Log.w(TAG, "Budget batch warning: ${e.message}")
                }
            }

            // Scheduled Expenses
            if (scheduled.isNotEmpty()) {
                val batch = db.batch()
                scheduled.forEach { s ->
                    val docId = if (s.id > 0) "se_${s.id}" else "se_${s.dueDate}_${s.title.hashCode()}"
                    val ref = userDoc.collection("scheduled_expenses").document(docId)
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
                        "isRecurringMonthly" to s.isRecurringMonthly,
                        "fundingSource" to s.fundingSource,
                        "extraordinaryFundId" to (s.extraordinaryFundId ?: 0L),
                        "extraordinaryFundTitle" to s.extraordinaryFundTitle,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(ref, data, SetOptions.merge())
                }
                try {
                    withTimeoutOrNull(4000L) { batch.commit().await() }
                } catch (e: Exception) {
                    Log.w(TAG, "Scheduled batch warning: ${e.message}")
                }
            }

            // Extraordinary Funds
            if (funds.isNotEmpty()) {
                val batch = db.batch()
                funds.forEach { f ->
                    val docId = if (f.id > 0) "ef_${f.id}" else "ef_${f.title.hashCode()}"
                    val ref = userDoc.collection("extraordinary_funds").document(docId)
                    val data = hashMapOf(
                        "id" to f.id,
                        "title" to f.title,
                        "totalAmount" to f.totalAmount,
                        "currencySymbol" to f.currencySymbol,
                        "note" to f.note,
                        "allocationsJson" to f.allocationsJson,
                        "createdAt" to f.createdAt,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(ref, data, SetOptions.merge())
                }
                try {
                    withTimeoutOrNull(4000L) { batch.commit().await() }
                } catch (e: Exception) {
                    Log.w(TAG, "Funds batch warning: ${e.message}")
                }
            }

            // Archived Periods (Estados de Cuenta / EC)
            if (archived.isNotEmpty()) {
                val batch = db.batch()
                archived.forEach { ap ->
                    val docId = if (ap.id > 0) "ap_${ap.id}" else "ap_${ap.archivedAt}_${ap.title.hashCode()}"
                    val ref = userDoc.collection("archived_periods").document(docId)
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
                    batch.set(ref, data, SetOptions.merge())
                }
                try {
                    withTimeoutOrNull(4000L) { batch.commit().await() }
                } catch (e: Exception) {
                    Log.w(TAG, "Archived batch warning: ${e.message}")
                }
            }

            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                isConnected = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                cloudTransactionsCount = transactions.size,
                cloudBudgetsCount = budgets.size,
                cloudScheduledCount = scheduled.size,
                syncStatusText = "🟢 Firebase Online (${transactions.size} movs guardados)"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to cloud: ${e.message}", e)
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                syncStatusText = "🟢 Firebase: Guardado localmente"
            )
        } finally {
            _syncStatus.value = _syncStatus.value.copy(isSyncing = false)
        }
    }
}
