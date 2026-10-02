package com.example.data

import android.content.Context
import com.example.data.dao.ArchivedPeriodDao
import com.example.data.dao.BudgetDao
import com.example.data.dao.ExtraordinaryFundDao
import com.example.data.dao.ScheduledExpenseDao
import com.example.data.dao.TransactionDao
import com.example.data.entity.ArchivedPeriodEntity
import com.example.data.entity.BudgetEntity
import com.example.data.entity.ExtraordinaryFundEntity
import com.example.data.entity.ScheduledExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.firebase.FirebaseFinanceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val scheduledExpenseDao: ScheduledExpenseDao,
    private val archivedPeriodDao: ArchivedPeriodDao,
    private val extraordinaryFundDao: ExtraordinaryFundDao,
    var firebaseManager: FirebaseFinanceManager? = null
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val allScheduledExpenses: Flow<List<ScheduledExpenseEntity>> = scheduledExpenseDao.getAllScheduledExpenses()
    val allArchivedPeriods: Flow<List<ArchivedPeriodEntity>> = archivedPeriodDao.getAllArchivedPeriods()
    val allExtraordinaryFunds: Flow<List<ExtraordinaryFundEntity>> = extraordinaryFundDao.getAllFunds()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        setupFirebaseListeners()
    }

    fun bindFirebaseManager(manager: FirebaseFinanceManager) {
        this.firebaseManager = manager
        setupFirebaseListeners()
    }

    private fun setupFirebaseListeners() {
        val manager = firebaseManager ?: return

        manager.onRemoteTransactionsReceived = { remoteTxs ->
            if (remoteTxs.isNotEmpty()) {
                repositoryScope.launch {
                    transactionDao.insertAll(remoteTxs)
                }
            }
        }

        manager.onRemoteBudgetsReceived = { remoteBudgets ->
            if (remoteBudgets.isNotEmpty()) {
                repositoryScope.launch {
                    budgetDao.insertAll(remoteBudgets)
                }
            }
        }

        manager.onRemoteScheduledReceived = { remoteScheduled ->
            if (remoteScheduled.isNotEmpty()) {
                repositoryScope.launch {
                    remoteScheduled.forEach { scheduledExpenseDao.insertScheduledExpense(it) }
                }
            }
        }

        manager.onRemoteFundsReceived = { remoteFunds ->
            if (remoteFunds.isNotEmpty()) {
                repositoryScope.launch {
                    remoteFunds.forEach { extraordinaryFundDao.insertFund(it) }
                }
            }
        }

        manager.onRemoteArchivedReceived = { remoteArchived ->
            if (remoteArchived.isNotEmpty()) {
                repositoryScope.launch {
                    remoteArchived.forEach { archivedPeriodDao.insertArchivedPeriod(it) }
                }
            }
        }
    }

    suspend fun archivePeriod(period: ArchivedPeriodEntity): Long {
        val id = archivedPeriodDao.insertArchivedPeriod(period)
        val entityWithId = if (period.id == 0L) period.copy(id = id) else period
        firebaseManager?.saveArchivedPeriodToCloud(entityWithId)
        return id
    }

    suspend fun updateArchivedPeriod(period: ArchivedPeriodEntity) {
        archivedPeriodDao.insertArchivedPeriod(period)
        firebaseManager?.saveArchivedPeriodToCloud(period)
    }

    suspend fun deleteArchivedPeriod(period: ArchivedPeriodEntity) {
        archivedPeriodDao.deleteArchivedPeriod(period)
        firebaseManager?.deleteArchivedPeriodFromCloud(period)
    }

    suspend fun addExtraordinaryFund(fund: ExtraordinaryFundEntity): Long {
        val id = extraordinaryFundDao.insertFund(fund)
        val fundWithId = if (fund.id == 0L) fund.copy(id = id) else fund
        firebaseManager?.saveExtraordinaryFundToCloud(fundWithId)
        return id
    }

    suspend fun updateExtraordinaryFund(fund: ExtraordinaryFundEntity) {
        extraordinaryFundDao.insertFund(fund)
        firebaseManager?.saveExtraordinaryFundToCloud(fund)
    }

    suspend fun deleteExtraordinaryFund(fund: ExtraordinaryFundEntity) {
        extraordinaryFundDao.deleteFund(fund)
        firebaseManager?.deleteExtraordinaryFundFromCloud(fund)
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        val id = transactionDao.insertTransaction(transaction)
        val txWithId = if (transaction.id == 0) transaction.copy(id = id.toInt()) else transaction
        firebaseManager?.saveTransactionToCloud(txWithId)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
        firebaseManager?.saveTransactionToCloud(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        firebaseManager?.deleteTransactionFromCloud(transaction)
    }

    suspend fun saveBudget(budget: BudgetEntity) {
        budgetDao.insertOrUpdateBudget(budget)
        firebaseManager?.saveBudgetToCloud(budget)
    }

    suspend fun deleteBudgetByCategory(category: String) {
        budgetDao.deleteBudgetByCategory(category)
        firebaseManager?.deleteBudgetFromCloud(category)
    }

    suspend fun addScheduledExpense(expense: ScheduledExpenseEntity): Long {
        val id = scheduledExpenseDao.insertScheduledExpense(expense)
        val expWithId = if (expense.id == 0L) expense.copy(id = id) else expense
        firebaseManager?.saveScheduledExpenseToCloud(expWithId)
        return id
    }

    suspend fun updateScheduledExpense(expense: ScheduledExpenseEntity) {
        scheduledExpenseDao.updateScheduledExpense(expense)
        firebaseManager?.saveScheduledExpenseToCloud(expense)
    }

    suspend fun deleteScheduledExpense(expense: ScheduledExpenseEntity) {
        scheduledExpenseDao.deleteScheduledExpense(expense)
        firebaseManager?.deleteScheduledExpenseFromCloud(expense)
    }

    suspend fun seedInitialDataIfEmpty(context: Context) {
        val prefs = context.getSharedPreferences("finanzas_clara_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("has_cleared_to_zero_v3", true)
            .putBoolean("has_seeded_initial_data_v2", true)
            .apply()
    }

    suspend fun clearLocalDataOnly() {
        transactionDao.deleteAll()
        budgetDao.deleteAll()
        scheduledExpenseDao.deleteAll()
        extraordinaryFundDao.deleteAll()
        archivedPeriodDao.deleteAll()
    }

    suspend fun fetchAndRestoreAllFromCloud(email: String, onPinRestored: ((String) -> Unit)? = null): Int {
        val manager = firebaseManager ?: return 0
        val snapshot = manager.fetchUserDataFromCloud(email)
        val totalRemote = snapshot.transactions.size + snapshot.budgets.size +
                snapshot.scheduledExpenses.size + snapshot.extraordinaryFunds.size +
                snapshot.archivedPeriods.size

        if (totalRemote > 0) {
            clearLocalDataOnly()
            if (!snapshot.userPin.isNullOrBlank()) {
                onPinRestored?.invoke(snapshot.userPin)
            }

            if (snapshot.transactions.isNotEmpty()) {
                transactionDao.insertAll(snapshot.transactions)
            }
            if (snapshot.budgets.isNotEmpty()) {
                budgetDao.insertAll(snapshot.budgets)
            }
            snapshot.scheduledExpenses.forEach {
                scheduledExpenseDao.insertScheduledExpense(it)
            }
            snapshot.extraordinaryFunds.forEach {
                extraordinaryFundDao.insertFund(it)
            }
            snapshot.archivedPeriods.forEach {
                archivedPeriodDao.insertArchivedPeriod(it)
            }
        } else if (!snapshot.userPin.isNullOrBlank()) {
            onPinRestored?.invoke(snapshot.userPin)
        }

        return totalRemote
    }

    suspend fun syncBidirectional(email: String, onPinRestored: ((String) -> Unit)? = null): Int {
        val manager = firebaseManager ?: return 0
        val txs = allTransactions.first()
        val bgs = allBudgets.first()
        val sched = allScheduledExpenses.first()
        val funds = allExtraordinaryFunds.first()
        val arch = allArchivedPeriods.first()

        val snapshot = manager.syncAllBidirectional(email, txs, bgs, sched, funds, arch)
        if (!snapshot.userPin.isNullOrBlank()) {
            onPinRestored?.invoke(snapshot.userPin)
        }
        if (snapshot.transactions.isNotEmpty()) {
            transactionDao.insertAll(snapshot.transactions)
        }
        if (snapshot.budgets.isNotEmpty()) {
            budgetDao.insertAll(snapshot.budgets)
        }
        snapshot.scheduledExpenses.forEach {
            scheduledExpenseDao.insertScheduledExpense(it)
        }
        snapshot.extraordinaryFunds.forEach {
            extraordinaryFundDao.insertFund(it)
        }
        snapshot.archivedPeriods.forEach {
            archivedPeriodDao.insertArchivedPeriod(it)
        }
        return snapshot.transactions.size + snapshot.budgets.size +
                snapshot.scheduledExpenses.size + snapshot.extraordinaryFunds.size +
                snapshot.archivedPeriods.size
    }

    fun saveUserPinToCloud(email: String, pin: String) {
        firebaseManager?.saveUserPin(email, pin)
    }

    fun publishVersionToCloud(version: String, notes: String, apkUrl: String) {
        firebaseManager?.publishVersionToCloud(version, notes, apkUrl)
    }

    suspend fun fetchUserPinFromCloud(email: String): String? {
        return firebaseManager?.fetchUserPin(email)
    }

    suspend fun clearAllData() {
        clearLocalDataOnly()
    }

    suspend fun clearAllDatabaseAndCloud(context: Context) {
        // 1. Wipe all local Room tables
        clearAllData()

        // 2. Wipe all remote Firestore collections for this user
        firebaseManager?.clearAllUserDataFromCloud()

        // 3. Mark preferences so sample data is never generated
        val prefs = context.getSharedPreferences("finanzas_clara_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("has_cleared_to_zero_v3", true)
            .putBoolean("has_seeded_initial_data_v2", true)
            .commit()
    }

    suspend fun syncAllToCloud() {
        val txs = allTransactions.first()
        val bgs = allBudgets.first()
        val sched = allScheduledExpenses.first()
        val funds = allExtraordinaryFunds.first()
        val arch = allArchivedPeriods.first()
        firebaseManager?.syncAllLocalToCloud(txs, bgs, sched, funds, arch)
    }

    suspend fun exportDataToJson(): String {
        val transactions = allTransactions.first()
        val budgets = allBudgets.first()
        val scheduled = allScheduledExpenses.first()
        val funds = allExtraordinaryFunds.first()
        val archived = allArchivedPeriods.first()

        val jsonRoot = JSONObject().apply {
            put("app", "FinanzasClara")
            put("version", 2)
            put("exportedAt", System.currentTimeMillis())
        }

        val txArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("title", tx.title)
                put("amount", tx.amount)
                put("category", tx.category)
                put("type", tx.type)
                put("timestamp", tx.timestamp)
                put("note", tx.note)
                put("isAiCategorized", tx.isAiCategorized)
                put("attachmentUri", tx.attachmentUri ?: "")
                put("dueDate", tx.dueDate ?: 0L)
                put("hasReminderScheduled", tx.hasReminderScheduled)
                put("originalAmount", tx.originalAmount)
                put("originalCurrency", tx.originalCurrency)
                put("exchangeRate", tx.exchangeRate)
            }
            txArray.put(obj)
        }

        val budgetArray = JSONArray()
        budgets.forEach { b ->
            val obj = JSONObject().apply {
                put("category", b.category)
                put("limitAmount", b.limitAmount)
                put("alertThresholdPercent", b.alertThresholdPercent)
            }
            budgetArray.put(obj)
        }

        val scheduledArray = JSONArray()
        scheduled.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("amount", s.amount)
                put("category", s.category)
                put("dueDate", s.dueDate)
                put("isPaid", s.isPaid)
                put("notifyReminder", s.notifyReminder)
                put("attachmentUri", s.attachmentUri ?: "")
                put("note", s.note)
                put("isEmergencyPriority", s.isEmergencyPriority)
            }
            scheduledArray.put(obj)
        }

        val fundArray = JSONArray()
        funds.forEach { f ->
            val obj = JSONObject().apply {
                put("id", f.id)
                put("title", f.title)
                put("totalAmount", f.totalAmount)
                put("note", f.note)
                put("allocationsJson", f.allocationsJson)
                put("createdAt", f.createdAt)
                put("currencySymbol", f.currencySymbol)
            }
            fundArray.put(obj)
        }

        val archivedArray = JSONArray()
        archived.forEach { ap ->
            val obj = JSONObject().apply {
                put("id", ap.id)
                put("title", ap.title)
                put("periodMode", ap.periodMode)
                put("archivedAt", ap.archivedAt)
                put("budgetLimit", ap.budgetLimit)
                put("totalSpent", ap.totalSpent)
                put("totalScheduled", ap.totalScheduled)
                put("currencySymbol", ap.currencySymbol)
                put("note", ap.note)
                put("transactionsJson", ap.transactionsJson)
                put("scheduledJson", ap.scheduledJson)
                put("isClosed", ap.isClosed)
            }
            archivedArray.put(obj)
        }

        jsonRoot.put("app", "FinanzasClara")
        jsonRoot.put("version", "1.1")
        jsonRoot.put("exportDate", System.currentTimeMillis())
        jsonRoot.put("transactions", txArray)
        jsonRoot.put("budgets", budgetArray)
        jsonRoot.put("scheduledExpenses", scheduledArray)
        jsonRoot.put("extraordinaryFunds", fundArray)
        jsonRoot.put("archivedPeriods", archivedArray)

        return jsonRoot.toString(2)
    }

    suspend fun importDataFromJson(jsonString: String): Boolean {
        return try {
            val jsonRoot = JSONObject(jsonString)
            if (jsonRoot.optString("app") != "FinanzasClara") return false

            val txArray = jsonRoot.optJSONArray("transactions") ?: JSONArray()
            val newTransactions = mutableListOf<TransactionEntity>()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                newTransactions.add(
                    TransactionEntity(
                        title = obj.getString("title"),
                        amount = obj.getDouble("amount"),
                        category = obj.getString("category"),
                        type = obj.getString("type"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        note = obj.optString("note", ""),
                        isAiCategorized = obj.optBoolean("isAiCategorized", false),
                        attachmentUri = obj.optString("attachmentUri").takeIf { it.isNotBlank() },
                        dueDate = obj.optLong("dueDate").takeIf { it > 0 },
                        hasReminderScheduled = obj.optBoolean("hasReminderScheduled", false),
                        originalAmount = obj.optDouble("originalAmount", obj.getDouble("amount")),
                        originalCurrency = obj.optString("originalCurrency", if (obj.optString("note", "").contains("USD")) "USD" else "NIO"),
                        exchangeRate = obj.optDouble("exchangeRate", 1.0)
                    )
                )
            }

            val budgetArray = jsonRoot.optJSONArray("budgets") ?: JSONArray()
            val newBudgets = mutableListOf<BudgetEntity>()
            for (i in 0 until budgetArray.length()) {
                val obj = budgetArray.getJSONObject(i)
                newBudgets.add(
                    BudgetEntity(
                        category = obj.getString("category"),
                        limitAmount = obj.getDouble("limitAmount"),
                        alertThresholdPercent = obj.optInt("alertThresholdPercent", 80)
                    )
                )
            }

            val scheduledArray = jsonRoot.optJSONArray("scheduledExpenses")
            val newScheduled = mutableListOf<ScheduledExpenseEntity>()
            if (scheduledArray != null) {
                for (i in 0 until scheduledArray.length()) {
                    val obj = scheduledArray.getJSONObject(i)
                    newScheduled.add(
                        ScheduledExpenseEntity(
                            title = obj.getString("title"),
                            amount = obj.getDouble("amount"),
                            category = obj.getString("category"),
                            dueDate = obj.getLong("dueDate"),
                            isPaid = obj.optBoolean("isPaid", false),
                            notifyReminder = obj.optBoolean("notifyReminder", true),
                            attachmentUri = obj.optString("attachmentUri").takeIf { it.isNotBlank() },
                            note = obj.optString("note", ""),
                            isEmergencyPriority = obj.optBoolean("isEmergencyPriority", false)
                        )
                    )
                }
            }

            val fundArray = jsonRoot.optJSONArray("extraordinaryFunds")
            val newFunds = mutableListOf<ExtraordinaryFundEntity>()
            if (fundArray != null) {
                for (i in 0 until fundArray.length()) {
                    val obj = fundArray.getJSONObject(i)
                    newFunds.add(
                        ExtraordinaryFundEntity(
                            title = obj.getString("title"),
                            totalAmount = obj.getDouble("totalAmount"),
                            note = obj.optString("note", ""),
                            allocationsJson = obj.optString("allocationsJson", "[]"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            currencySymbol = obj.optString("currencySymbol", "C$")
                        )
                    )
                }
            }

            val archivedArray = jsonRoot.optJSONArray("archivedPeriods")
            val newArchived = mutableListOf<ArchivedPeriodEntity>()
            if (archivedArray != null) {
                for (i in 0 until archivedArray.length()) {
                    val obj = archivedArray.getJSONObject(i)
                    newArchived.add(
                        ArchivedPeriodEntity(
                            title = obj.getString("title"),
                            periodMode = obj.optString("periodMode", "MONTHLY"),
                            archivedAt = obj.optLong("archivedAt", System.currentTimeMillis()),
                            budgetLimit = obj.optDouble("budgetLimit", 0.0),
                            totalSpent = obj.optDouble("totalSpent", 0.0),
                            totalScheduled = obj.optDouble("totalScheduled", 0.0),
                            currencySymbol = obj.optString("currencySymbol", "C$"),
                            note = obj.optString("note", ""),
                            transactionsJson = obj.optString("transactionsJson", "[]"),
                            scheduledJson = obj.optString("scheduledJson", "[]"),
                            isClosed = obj.optBoolean("isClosed", true)
                        )
                    )
                }
            }

            clearLocalDataOnly()

            if (newTransactions.isNotEmpty()) transactionDao.insertAll(newTransactions)
            if (newBudgets.isNotEmpty()) budgetDao.insertAll(newBudgets)
            newScheduled.forEach { scheduledExpenseDao.insertScheduledExpense(it) }
            newFunds.forEach { extraordinaryFundDao.insertFund(it) }
            newArchived.forEach { archivedPeriodDao.insertArchivedPeriod(it) }

            // Push imported data to Firebase
            firebaseManager?.syncAllLocalToCloud(newTransactions, newBudgets, newScheduled, newFunds, newArchived)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
