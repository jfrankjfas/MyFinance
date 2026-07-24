package com.example.data

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val scheduledExpenseDao: ScheduledExpenseDao,
    private val archivedPeriodDao: ArchivedPeriodDao,
    private val extraordinaryFundDao: ExtraordinaryFundDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val allScheduledExpenses: Flow<List<ScheduledExpenseEntity>> = scheduledExpenseDao.getAllScheduledExpenses()
    val allArchivedPeriods: Flow<List<ArchivedPeriodEntity>> = archivedPeriodDao.getAllArchivedPeriods()
    val allExtraordinaryFunds: Flow<List<ExtraordinaryFundEntity>> = extraordinaryFundDao.getAllFunds()

    suspend fun archivePeriod(period: ArchivedPeriodEntity): Long {
        return archivedPeriodDao.insertArchivedPeriod(period)
    }

    suspend fun updateArchivedPeriod(period: ArchivedPeriodEntity) {
        archivedPeriodDao.insertArchivedPeriod(period)
    }

    suspend fun deleteArchivedPeriod(period: ArchivedPeriodEntity) {
        archivedPeriodDao.deleteArchivedPeriod(period)
    }

    suspend fun addExtraordinaryFund(fund: ExtraordinaryFundEntity): Long {
        return extraordinaryFundDao.insertFund(fund)
    }

    suspend fun updateExtraordinaryFund(fund: ExtraordinaryFundEntity) {
        extraordinaryFundDao.insertFund(fund)
    }

    suspend fun deleteExtraordinaryFund(fund: ExtraordinaryFundEntity) {
        extraordinaryFundDao.deleteFund(fund)
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun saveBudget(budget: BudgetEntity) {
        budgetDao.insertOrUpdateBudget(budget)
    }

    suspend fun addScheduledExpense(expense: ScheduledExpenseEntity): Long {
        return scheduledExpenseDao.insertScheduledExpense(expense)
    }

    suspend fun updateScheduledExpense(expense: ScheduledExpenseEntity) {
        scheduledExpenseDao.updateScheduledExpense(expense)
    }

    suspend fun deleteScheduledExpense(expense: ScheduledExpenseEntity) {
        scheduledExpenseDao.deleteScheduledExpense(expense)
    }

    suspend fun seedInitialDataIfEmpty() {
        val currentTxList = allTransactions.first()
        if (currentTxList.isEmpty()) {
            val now = System.currentTimeMillis()
            val day = 86400000L

            val sampleTransactions = listOf(
                TransactionEntity(
                    title = "Sueldo Mensual",
                    amount = 1800.0,
                    category = "Sueldo",
                    type = "INCOME",
                    timestamp = now - (2 * day),
                    note = "Depósito nómina empresa"
                ),
                TransactionEntity(
                    title = "Supermercado Semanal",
                    amount = 125.50,
                    category = "Alimentación",
                    type = "EXPENSE",
                    timestamp = now - (1 * day),
                    note = "Compra de abarrotes y verduras"
                ),
                TransactionEntity(
                    title = "Carga de Gasolina",
                    amount = 45.0,
                    category = "Transporte",
                    type = "EXPENSE",
                    timestamp = now - (12 * 3600000L),
                    note = "Estación YPF"
                ),
                TransactionEntity(
                    title = "Cine y Snacks",
                    amount = 28.0,
                    category = "Entretenimiento",
                    type = "EXPENSE",
                    timestamp = now - (6 * 3600000L),
                    note = "Entradas para 2 personas",
                    isAiCategorized = true
                ),
                TransactionEntity(
                    title = "Servicio de Internet",
                    amount = 35.0,
                    category = "Servicios",
                    type = "EXPENSE",
                    timestamp = now - (3 * day),
                    note = "Factura del mes"
                )
            )
            transactionDao.insertAll(sampleTransactions)

            val sampleBudgets = listOf(
                BudgetEntity(category = "GENERAL", limitAmount = 1200.0, alertThresholdPercent = 80),
                BudgetEntity(category = "Alimentación", limitAmount = 400.0, alertThresholdPercent = 80),
                BudgetEntity(category = "Transporte", limitAmount = 150.0, alertThresholdPercent = 80),
                BudgetEntity(category = "Entretenimiento", limitAmount = 120.0, alertThresholdPercent = 80),
                BudgetEntity(category = "Servicios", limitAmount = 100.0, alertThresholdPercent = 80)
            )
            budgetDao.insertAll(sampleBudgets)

            val sampleScheduled = listOf(
                ScheduledExpenseEntity(
                    title = "Pago de Alquiler / Renta",
                    amount = 350.0,
                    category = "Vivienda",
                    dueDate = now + (3 * day),
                    isPaid = false,
                    notifyReminder = true
                ),
                ScheduledExpenseEntity(
                    title = "Servicio de Luz (ENEL)",
                    amount = 42.0,
                    category = "Servicios",
                    dueDate = now + (7 * day),
                    isPaid = false,
                    notifyReminder = true
                )
            )
            sampleScheduled.forEach { scheduledExpenseDao.insertScheduledExpense(it) }
        }
    }

    suspend fun clearAllData() {
        transactionDao.deleteAll()
        budgetDao.deleteAll()
        scheduledExpenseDao.deleteAll()
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
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
