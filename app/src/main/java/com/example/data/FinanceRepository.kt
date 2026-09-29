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

        return snapshot.transactions.size + snapshot.budgets.size +
                snapshot.scheduledExpenses.size + snapshot.extraordinaryFunds.size +
                snapshot.archivedPeriods.size
    }

    fun saveUserPinToCloud(email: String, pin: String) {
        firebaseManager?.saveUserPin(email, pin)
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

        val jsonRoot = JSONObject()

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

        jsonRoot.put("app", "FinanzasClara")
        jsonRoot.put("version", "1.0")
        jsonRoot.put("exportDate", System.currentTimeMillis())
        jsonRoot.put("transactions", txArray)
        jsonRoot.put("budgets", budgetArray)

        return jsonRoot.toString(2)
    }

    suspend fun importDataFromJson(jsonString: String): Boolean {
        return try {
            val jsonRoot = JSONObject(jsonString)
            if (jsonRoot.optString("app") != "FinanzasClara") return false

            val txArray = jsonRoot.getJSONArray("transactions")
            val newTransactions = mutableListOf<TransactionEntity>()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                newTransactions.add(
                    TransactionEntity(
                        title = obj.getString("title"),
                        amount = obj.getDouble("amount"),
                        category = obj.getString("category"),
                        type = obj.getString("type"),
                        timestamp = obj.getLong("timestamp"),
                        note = obj.optString("note", ""),
                        isAiCategorized = obj.optBoolean("isAiCategorized", false)
                    )
                )
            }

            val budgetArray = jsonRoot.getJSONArray("budgets")
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

            transactionDao.deleteAll()
            budgetDao.deleteAll()

            transactionDao.insertAll(newTransactions)
            budgetDao.insertAll(newBudgets)

            // Push imported data to Firebase
            newTransactions.forEach { firebaseManager?.saveTransactionToCloud(it) }
            newBudgets.forEach { firebaseManager?.saveBudgetToCloud(it) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
