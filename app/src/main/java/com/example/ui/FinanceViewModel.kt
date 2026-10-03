package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiCategorizedResult
import com.example.ai.GeminiCategorizer
import com.example.ai.ReceiptScanResult
import com.example.auth.GoogleIdentityManager
import com.example.data.AppDatabase
import com.example.data.FinanceRepository
import com.example.data.entity.ArchivedPeriodEntity
import com.example.data.entity.BudgetEntity
import com.example.data.entity.ScheduledExpenseEntity
import com.example.data.entity.TransactionEntity
import com.example.data.firebase.FirebaseFinanceManager
import com.example.data.firebase.FirebaseSyncStatus
import com.example.notification.NotificationHelper
import com.example.notification.PaymentAlarmScheduler
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.data.model.CurrencyItem
import com.example.data.model.WorldCurrencies
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategoryExpense(
    val category: String,
    val totalAmount: Double,
    val percentage: Float
)

data class BudgetProgress(
    val category: String,
    val limitAmount: Double,
    val spentAmount: Double,
    val pendingAmount: Double = 0.0,
    val percentage: Int,
    val isExceeded: Boolean,
    val isWarning: Boolean,
    val subtitle: String = "",
    val carryoverNotice: String = ""
)

data class CurrencyConfig(
    val favs: List<CurrencyItem>,
    val activeIndex: Int,
    val rate2: Double,
    val rate3: Double,
    val multiplier: Double,
    val symbol: String
)

data class UserSettingsState(
    val userEmail: String = "",
    val userName: String = "",
    val isGoogleDriveConnected: Boolean = false,
    val isLoggedIn: Boolean = false,
    val lastDriveSync: String? = null,
    val csvExportData: String? = null,
    val favoriteCurrencies: List<CurrencyItem> = WorldCurrencies.DEFAULT_3,
    val appTheme: String = "SYSTEM",
    val appLanguage: String = "ES"
)

data class AiState(
    val isAiLoading: Boolean = false,
    val aiErrorMessage: String? = null,
    val backupJson: String? = null,
    val importMessage: String? = null,
    val availableUpdate: com.example.update.UpdateInfo? = null
)

data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val rawTransactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val scheduledExpenses: List<ScheduledExpenseEntity> = emptyList(),
    val periodScheduledExpenses: List<ScheduledExpenseEntity> = emptyList(),
    val archivedPeriods: List<ArchivedPeriodEntity> = emptyList(),
    val extraordinaryFunds: List<com.example.data.entity.ExtraordinaryFundEntity> = emptyList(),
    val budgetPeriodMode: String = "MONTHLY", // "MONTHLY", "FORTNIGHT_1", "FORTNIGHT_2"
    val periodPendingExpensesTotal: Double = 0.0,
    val periodSpentTotal: Double = 0.0,
    val periodBudgetLimitTotal: Double = 0.0,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categoryExpenses: List<CategoryExpense> = emptyList(),
    val budgetProgresses: List<BudgetProgress> = emptyList(),
    val currencySymbol: String = "C$",
    val favoriteCurrencies: List<CurrencyItem> = WorldCurrencies.DEFAULT_3,
    val activeCurrencyIndex: Int = 0, // Default 0 (NIO C$ - Moneda Principal)
    val exchangeRate2: Double = 36.6243, // 1 USD = 36.6243 C$
    val exchangeRate3: Double = 39.809,  // 1 EUR = 39.809 C$
    val activeConversionMultiplier: Double = 1.0,
    val userEmail: String = "",
    val userName: String = "",
    val isGoogleDriveConnected: Boolean = false,
    val isLoggedIn: Boolean = false,
    val lastDriveSync: String? = null,
    val csvExportData: String? = null,
    val appTheme: String = "SYSTEM",
    val appLanguage: String = "ES",
    val isAiLoading: Boolean = false,
    val aiErrorMessage: String? = null,
    val backupJson: String? = null,
    val importMessage: String? = null,
    val firebaseSyncStatus: FirebaseSyncStatus = FirebaseSyncStatus(),
    val monthlyDeficit: Double = 0.0,
    val isBankruptcyAlert: Boolean = false,
    val frozenExpensesSavings: Double = 0.0,
    val availableUpdate: com.example.update.UpdateInfo? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val firebaseManager = FirebaseFinanceManager(application, viewModelScope)
    private val repository = FinanceRepository(
        db.transactionDao(),
        db.budgetDao(),
        db.scheduledExpenseDao(),
        db.archivedPeriodDao(),
        db.extraordinaryFundDao(),
        firebaseManager
    )

    private val appPrefs = application.getSharedPreferences("finanzas_clara_prefs", android.content.Context.MODE_PRIVATE)
    private val initialIsLoggedIn = appPrefs.getBoolean("is_logged_in", false)
    private val initialEmail = if (initialIsLoggedIn) appPrefs.getString("saved_email", "") ?: "" else ""
    private val initialName = if (initialIsLoggedIn) appPrefs.getString("saved_name", "") ?: "" else ""
    private val initialCurrencyIndex = appPrefs.getInt("active_currency_index", 0)
    private val initialExchangeRate2 = appPrefs.getString("exchange_rate_2", "36.6243")?.toDoubleOrNull() ?: 36.6243
    private val initialExchangeRate3 = appPrefs.getString("exchange_rate_3", "39.809")?.toDoubleOrNull() ?: 39.809
    private val initialCurrencySymbol = appPrefs.getString("currency_symbol", if (initialCurrencyIndex == 1) "$" else "C$") ?: "C$"

    private val _budgetPeriodMode = MutableStateFlow("MONTHLY") // "MONTHLY", "FORTNIGHT_1", "FORTNIGHT_2"
    val budgetPeriodMode: StateFlow<String> = _budgetPeriodMode.asStateFlow()

    private val _favoriteCurrencies = MutableStateFlow<List<CurrencyItem>>(WorldCurrencies.DEFAULT_3)
    val favoriteCurrencies: StateFlow<List<CurrencyItem>> = _favoriteCurrencies.asStateFlow()

    private val _activeCurrencyIndex = MutableStateFlow(initialCurrencyIndex)
    val activeCurrencyIndex: StateFlow<Int> = _activeCurrencyIndex.asStateFlow()

    private val _exchangeRate2 = MutableStateFlow(initialExchangeRate2)
    val exchangeRate2: StateFlow<Double> = _exchangeRate2.asStateFlow()

    private val _exchangeRate3 = MutableStateFlow(initialExchangeRate3)
    val exchangeRate3: StateFlow<Double> = _exchangeRate3.asStateFlow()

    private val _currencySymbol = MutableStateFlow(initialCurrencySymbol)
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _userEmail = MutableStateFlow(initialEmail)
    private val _userName = MutableStateFlow(initialName)
    private val _isGoogleDriveConnected = MutableStateFlow(initialIsLoggedIn)
    private val _lastDriveSync = MutableStateFlow<String?>(if (initialIsLoggedIn) "Conectado a cuenta Google" else "Sesión cerrada")
    private val _csvExportData = MutableStateFlow<String?>(null)
    private val _appTheme = MutableStateFlow("SYSTEM") // SYSTEM, LIGHT, DARK
    private val _appLanguage = MutableStateFlow("ES") // ES, EN

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage: StateFlow<String?> = _aiErrorMessage.asStateFlow()

    private val _backupJson = MutableStateFlow<String?>(null)
    val backupJson: StateFlow<String?> = _backupJson.asStateFlow()

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage.asStateFlow()

    private val _availableUpdate = MutableStateFlow<com.example.update.UpdateInfo?>(null)
    val availableUpdate: StateFlow<com.example.update.UpdateInfo?> = _availableUpdate.asStateFlow()

    private val _userSettingsState = combine(
        _userEmail,
        _userName,
        _isGoogleDriveConnected,
        _lastDriveSync,
        _csvExportData,
        _appTheme,
        _appLanguage
    ) { args ->
        UserSettingsState(
            userEmail = args[0] as String,
            userName = args[1] as String,
            isGoogleDriveConnected = args[2] as Boolean,
            isLoggedIn = (args[2] as Boolean) && (args[0] as String).isNotBlank(),
            lastDriveSync = args[3] as? String,
            csvExportData = args[4] as? String,
            favoriteCurrencies = _favoriteCurrencies.value,
            appTheme = args[5] as String,
            appLanguage = args[6] as String
        )
    }

    private val _aiState = combine(
        _isAiLoading,
        _aiErrorMessage,
        _backupJson,
        _importMessage,
        _availableUpdate
    ) { loading, err, backup, impMsg, updateInfo ->
        AiState(loading, err, backup, impMsg, updateInfo)
    }

    private val _currencyConfigState = combine(
        _favoriteCurrencies,
        _activeCurrencyIndex,
        _exchangeRate2,
        _exchangeRate3
    ) { favs, idx, rate2, rate3 ->
        val safeIndex = idx.coerceIn(0, 2)
        val selectedItem = favs.getOrElse(safeIndex) { WorldCurrencies.DEFAULT_3[0] }
        // Multiplicador para pasar de CÓRDOBAS (moneda base guardada) a la moneda de visualización:
        // - Si visualiza en Córdobas (0): multiplier = 1.0 (exacto, sin redondeos ni fluctuación)
        // - Si visualiza en Dólares (1): 1 USD = rate2 C$, por lo que C$ / rate2 -> multiplier = 1.0 / rate2
        // - Si visualiza en Euros (2): 1 EUR = rate3 C$, por lo que C$ / rate3 -> multiplier = 1.0 / rate3
        val multiplier = when (safeIndex) {
            0 -> 1.0
            1 -> if (rate2 > 0) 1.0 / rate2 else 1.0
            2 -> if (rate3 > 0) 1.0 / rate3 else 1.0
            else -> 1.0
        }
        val symbol = selectedItem.symbol
        
        CurrencyConfig(
            favs = favs,
            activeIndex = safeIndex,
            rate2 = rate2,
            rate3 = rate3,
            multiplier = multiplier,
            symbol = symbol
        )
    }

    /**
     * Convierte cualquier monto ingresado a la moneda principal CÓRDOBAS (NIO C$).
     * - Si se registra en Córdobas (C$): el monto se guarda exactamente sin modificación.
     * - Si se registra en Dólares (USD): se calcula Córdobas = Dólares * Tasa (36.6243).
     * El primer cálculo de Córdobas es el que manda y queda guardado como base inmutable en Room/Firestore.
     */
    fun convertToCordobas(amount: Double): Double {
        val idx = _activeCurrencyIndex.value.coerceIn(0, 2)
        return when (idx) {
            0 -> amount // Córdobas C$ (Moneda principal): exacto, sin divisiones ni errores de redondeo
            1 -> {
                val r2 = _exchangeRate2.value
                if (r2 > 0) amount * r2 else amount
            }
            2 -> {
                val r3 = _exchangeRate3.value
                if (r3 > 0) amount * r3 else amount
            }
            else -> amount
        }
    }

    private fun isTimestampInPeriod(timestamp: Long, mode: String): Boolean {
        val cal = Calendar.getInstance()
        val curMonth = cal.get(Calendar.MONTH)
        val curYear = cal.get(Calendar.YEAR)

        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        if (targetCal.get(Calendar.MONTH) != curMonth || targetCal.get(Calendar.YEAR) != curYear) {
            return false
        }
        val day = targetCal.get(Calendar.DAY_OF_MONTH)
        return when (mode) {
            "FORTNIGHT_1" -> day in 1..15
            "FORTNIGHT_2" -> day >= 16
            else -> true // MONTHLY
        }
    }

    val uiState: StateFlow<FinanceUiState> = combine(
        repository.allTransactions,
        repository.allBudgets,
        repository.allScheduledExpenses,
        repository.allArchivedPeriods,
        repository.allExtraordinaryFunds,
        _budgetPeriodMode,
        _currencyConfigState,
        _userSettingsState,
        _aiState,
        firebaseManager.syncStatus
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val txs = args[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val budgets = args[1] as List<BudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val scheduledList = args[2] as List<ScheduledExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val archivedPeriodsList = args[3] as List<ArchivedPeriodEntity>
        @Suppress("UNCHECKED_CAST")
        val extraFundsList = args[4] as List<com.example.data.entity.ExtraordinaryFundEntity>
        val periodMode = args[5] as String
        val currConfig = args[6] as CurrencyConfig
        val settings = args[7] as UserSettingsState
        val aiState = args[8] as AiState
        val fbSync = args[9] as FirebaseSyncStatus

        var rawIncome = 0.0
        var rawExpense = 0.0
        val rawCategoryMap = mutableMapOf<String, Double>()

        txs.forEach { tx ->
            if (tx.type == "INCOME") {
                rawIncome += tx.amount
            } else {
                rawExpense += tx.amount
                rawCategoryMap[tx.category] = (rawCategoryMap[tx.category] ?: 0.0) + tx.amount
            }
        }

        val multiplier = currConfig.multiplier
        val activeIncome = rawIncome * multiplier
        val activeExpense = rawExpense * multiplier
        val activeBalance = activeIncome - activeExpense

        // Period calculations
        val periodTxs = txs.filter { isTimestampInPeriod(it.timestamp, periodMode) }
        var periodRawExpense = 0.0
        val periodCategoryMap = mutableMapOf<String, Double>()
        periodTxs.forEach { tx ->
            if (tx.type == "EXPENSE") {
                val isCoveredExternally = tx.note.contains("[Pagado de Fondo Extraordinario") || tx.note.contains("[Pagado con Ahorros")
                if (!isCoveredExternally) {
                    periodRawExpense += tx.amount
                    periodCategoryMap[tx.category] = (periodCategoryMap[tx.category] ?: 0.0) + tx.amount
                }
            }
        }
        val periodActiveExpense = periodRawExpense * multiplier

        // Scheduled expenses filtering for active period (only period budget funded expenses count against period budget)
        val periodScheduled = scheduledList.filter { isTimestampInPeriod(it.dueDate, periodMode) }
        val periodPendingScheduled = periodScheduled.filter { !it.isPaid && it.fundingSource != "EXTRAORDINARY_FUND" && it.fundingSource != "SAVINGS" }
        val periodPendingRawTotal = periodPendingScheduled.sumOf { it.amount }
        val periodPendingActiveTotal = periodPendingRawTotal * multiplier

        // Converted transactions list (sorted with newest first)
        val convertedTxs = txs.sortedByDescending { it.timestamp }.map { tx ->
            tx.copy(amount = tx.amount * multiplier)
        }

        // Converted scheduled expenses (sorted chronologically by due date)
        val convertedScheduled = scheduledList.sortedBy { it.dueDate }.map { s ->
            s.copy(amount = s.amount * multiplier)
        }

        val convertedPeriodScheduled = periodScheduled.sortedBy { it.dueDate }.map { s ->
            s.copy(amount = s.amount * multiplier)
        }

        // Converted extraordinary funds list with amounts matching the active currency (C$ or USD)
        val convertedExtraFunds = extraFundsList.map { fund ->
            val convertedAllocationsJson = try {
                val arr = JSONArray(fund.allocationsJson)
                val newArr = JSONArray()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val copyObj = JSONObject(obj.toString())
                    val rawAmt = obj.optDouble("amount", 0.0)
                    copyObj.put("amount", rawAmt * multiplier)
                    newArr.put(copyObj)
                }
                newArr.toString()
            } catch (e: Exception) {
                fund.allocationsJson
            }

            fund.copy(
                totalAmount = fund.totalAmount * multiplier,
                currencySymbol = currConfig.symbol,
                allocationsJson = convertedAllocationsJson
            )
        }

        val catExpensesList = if (activeExpense > 0) {
            rawCategoryMap.map { (cat, amt) ->
                val convertedAmt = amt * multiplier
                CategoryExpense(
                    category = cat,
                    totalAmount = convertedAmt,
                    percentage = ((amt / rawExpense) * 100).toFloat()
                )
            }.sortedByDescending { it.totalAmount }
        } else {
            emptyList()
        }

        val budgetProgressList = mutableListOf<BudgetProgress>()

        val generalBudget = budgets.find { it.category == "GENERAL" }
        var periodGeneralBudgetLimit = 0.0

        if (generalBudget != null && generalBudget.limitAmount > 0) {
            val monthlyBaseLimit = generalBudget.limitAmount
            val fortnightBaseLimit = monthlyBaseLimit / 2.0

            // Fortnight 1 metrics
            val q1Txs = txs.filter { isTimestampInPeriod(it.timestamp, "FORTNIGHT_1") && it.type == "EXPENSE" && !it.note.contains("[Pagado de Fondo Extraordinario") && !it.note.contains("[Pagado con Ahorros") }
            val q1SpentRaw = q1Txs.sumOf { it.amount }
            val q1PendingRaw = scheduledList.filter { isTimestampInPeriod(it.dueDate, "FORTNIGHT_1") && !it.isPaid && it.fundingSource != "EXTRAORDINARY_FUND" && it.fundingSource != "SAVINGS" }.sumOf { it.amount }
            val q1CommittedRaw = q1SpentRaw + q1PendingRaw
            val q1Limit = fortnightBaseLimit * multiplier
            val q1Committed = q1CommittedRaw * multiplier
            val q1Spent = q1SpentRaw * multiplier
            val q1Pending = q1PendingRaw * multiplier

            // Fortnight 2 metrics
            val q2Txs = txs.filter { isTimestampInPeriod(it.timestamp, "FORTNIGHT_2") && it.type == "EXPENSE" && !it.note.contains("[Pagado de Fondo Extraordinario") && !it.note.contains("[Pagado con Ahorros") }
            val q2SpentRaw = q2Txs.sumOf { it.amount }
            val q2PendingRaw = scheduledList.filter { isTimestampInPeriod(it.dueDate, "FORTNIGHT_2") && !it.isPaid && it.fundingSource != "EXTRAORDINARY_FUND" && it.fundingSource != "SAVINGS" }.sumOf { it.amount }
            val q2CommittedRaw = q2SpentRaw + q2PendingRaw
            val q2Limit = fortnightBaseLimit * multiplier
            val q2Committed = q2CommittedRaw * multiplier
            val q2Spent = q2SpentRaw * multiplier
            val q2Pending = q2PendingRaw * multiplier

            // Automatic continuation: when 1ª Quincena reaches 100%, excess continues towards 2ª Quincena
            val q1Excess = (q1Committed - q1Limit).coerceAtLeast(0.0)

            if (periodMode == "FORTNIGHT_1" || periodMode == "FORTNIGHT_2") {
                periodGeneralBudgetLimit = if (periodMode == "FORTNIGHT_1") q1Limit else q2Limit

                val q1Pct = if (q1Limit > 0) ((q1Committed / q1Limit) * 100).toInt() else 0
                val q1CarryNotice = if (q1Excess > 0) {
                    "Excedente de ${currConfig.symbol}${String.format(Locale.US, "%.2f", q1Excess)} continúa hacia la 2ª Quincena"
                } else ""

                budgetProgressList.add(
                    BudgetProgress(
                        category = "Presupuesto 1ª Quincena (1-15)",
                        limitAmount = q1Limit,
                        spentAmount = minOf(q1Limit, q1Spent),
                        pendingAmount = if (q1Spent >= q1Limit) 0.0 else minOf(q1Limit - q1Spent, q1Pending),
                        percentage = minOf(100, q1Pct),
                        isExceeded = q1Pct >= 100 && (q1Committed + q2Committed) >= (q1Limit + q2Limit),
                        isWarning = q1Pct >= generalBudget.alertThresholdPercent,
                        subtitle = if (q1Pct >= 100) "100% alcanzado • Excedente continúa en 2ª Quincena" else "Límite: ${currConfig.symbol}${String.format(Locale.US, "%.2f", q1Limit)}",
                        carryoverNotice = q1CarryNotice
                    )
                )

                val q2EffectiveCommitted = q2Committed + q1Excess
                val q2Pct = if (q2Limit > 0) ((q2EffectiveCommitted / q2Limit) * 100).toInt() else 0
                val q2CarryNotice = if (q1Excess > 0) {
                    "Continúa absorbiendo ${currConfig.symbol}${String.format(Locale.US, "%.2f", q1Excess)} de la 1ª Quincena"
                } else ""

                budgetProgressList.add(
                    BudgetProgress(
                        category = "Presupuesto 2ª Quincena (16-Fin)",
                        limitAmount = q2Limit,
                        spentAmount = q2Spent + q1Excess,
                        pendingAmount = q2Pending,
                        percentage = q2Pct,
                        isExceeded = q2Pct >= 100,
                        isWarning = q2Pct >= generalBudget.alertThresholdPercent,
                        subtitle = if (q1Excess > 0) "Incluye excedente continuo de 1ª Quincena (${currConfig.symbol}${String.format(Locale.US, "%.2f", q1Excess)})" else "Límite: ${currConfig.symbol}${String.format(Locale.US, "%.2f", q2Limit)}",
                        carryoverNotice = q2CarryNotice
                    )
                )
            } else {
                // Monthly Mode
                val convertedLimit = monthlyBaseLimit * multiplier
                periodGeneralBudgetLimit = convertedLimit
                val totalCommittedRaw = periodRawExpense + periodPendingRawTotal
                val pct = ((totalCommittedRaw / monthlyBaseLimit) * 100).toInt()

                budgetProgressList.add(
                    BudgetProgress(
                        category = "Presupuesto Total Mensual",
                        limitAmount = convertedLimit,
                        spentAmount = periodActiveExpense,
                        pendingAmount = periodPendingActiveTotal,
                        percentage = pct,
                        isExceeded = pct >= 100,
                        isWarning = pct >= generalBudget.alertThresholdPercent,
                        subtitle = "Presupuesto global del periodo",
                        carryoverNotice = if (q1Excess > 0) "1ª Quincena completó el 100% y continuó hacia la 2ª Quincena" else ""
                    )
                )
            }
        }

        budgets.filter { it.category != "GENERAL" }.forEach { b ->
            val catSpentRaw = periodCategoryMap[b.category] ?: 0.0
            val catPendingRaw = periodPendingScheduled.filter { it.category == b.category }.sumOf { it.amount }
            val catCommittedRaw = catSpentRaw + catPendingRaw

            val catBaseLimit = if (periodMode == "MONTHLY") b.limitAmount else (b.limitAmount / 2.0)
            val convertedSpent = catSpentRaw * multiplier
            val convertedPending = catPendingRaw * multiplier
            val convertedLimit = catBaseLimit * multiplier

            val pct = if (catBaseLimit > 0) ((catCommittedRaw / catBaseLimit) * 100).toInt() else 0

            budgetProgressList.add(
                BudgetProgress(
                    category = b.category,
                    limitAmount = convertedLimit,
                    spentAmount = convertedSpent,
                    pendingAmount = convertedPending,
                    percentage = pct,
                    isExceeded = pct >= 100,
                    isWarning = pct >= b.alertThresholdPercent,
                    subtitle = if (pct >= 100) "Límite superado • Continúa cubierto por el presupuesto del periodo" else "",
                    carryoverNotice = if (pct >= 100) "Excedente absorbido por el presupuesto del periodo" else ""
                )
            )
        }

        val deficit = (activeExpense + periodPendingActiveTotal) - activeIncome
        val isBankruptcy = deficit > 0.01 || (activeExpense > activeIncome && activeIncome > 0)

        FinanceUiState(
            transactions = convertedTxs,
            rawTransactions = txs,
            budgets = budgets,
            scheduledExpenses = convertedScheduled,
            periodScheduledExpenses = convertedPeriodScheduled,
            archivedPeriods = archivedPeriodsList,
            extraordinaryFunds = convertedExtraFunds,
            budgetPeriodMode = periodMode,
            periodPendingExpensesTotal = periodPendingActiveTotal,
            periodSpentTotal = periodActiveExpense,
            periodBudgetLimitTotal = periodGeneralBudgetLimit,
            totalBalance = activeBalance,
            totalIncome = activeIncome,
            totalExpense = activeExpense,
            categoryExpenses = catExpensesList,
            budgetProgresses = budgetProgressList,
            currencySymbol = currConfig.symbol,
            favoriteCurrencies = currConfig.favs,
            activeCurrencyIndex = currConfig.activeIndex,
            exchangeRate2 = currConfig.rate2,
            exchangeRate3 = currConfig.rate3,
            activeConversionMultiplier = currConfig.multiplier,
            userEmail = settings.userEmail,
            userName = settings.userName,
            isGoogleDriveConnected = settings.isGoogleDriveConnected,
            isLoggedIn = settings.isLoggedIn,
            lastDriveSync = settings.lastDriveSync,
            csvExportData = settings.csvExportData,
            appTheme = settings.appTheme,
            appLanguage = settings.appLanguage,
            isAiLoading = aiState.isAiLoading,
            aiErrorMessage = aiState.aiErrorMessage,
            backupJson = aiState.backupJson,
            importMessage = aiState.importMessage,
            firebaseSyncStatus = fbSync,
            monthlyDeficit = if (deficit > 0) deficit else 0.0,
            isBankruptcyAlert = isBankruptcy,
            availableUpdate = aiState.availableUpdate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState()
    )

    init {
        repository.bindFirebaseManager(firebaseManager)
        // Ensure Firestore cloud version metadata is set to v2.1 for all active devices
        repository.publishVersionToCloud(
            version = "2.1",
            notes = "Versión 2.1: Continuación automática de presupuesto entre quincenas al superar el 100%, selección de origen de fondos (Presupuesto o Fondos Extraordinarios) en Gastos Programados, soporte completo para pagos recurrentes mensuales con renovación automática y corrección en la edición/eliminación de registros e inversiones.",
            apkUrl = "https://github.com/jfrankjfas/MyFinance/releases/download/v2.1/FinanzasClaras-v2.1.apk"
        )
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty(getApplication())
            if (initialIsLoggedIn && initialEmail.isNotBlank()) {
                firebaseManager.updateUserAccount(initialEmail)
                // Fetch and restore whatever is already linked to this Gmail account from Firestore
                try {
                    val count = repository.syncBidirectional(initialEmail)
                    if (count > 0) {
                        _importMessage.value = "✅ Se sincronizaron $count registros vinculados a tu cuenta ($initialEmail)."
                    }
                } catch (e: Exception) {
                    Log.w("FinanceViewModel", "Init sync warning: ${e.message}")
                }
            } else {
                firebaseManager.disconnectUser()
            }
            checkUpcomingPaymentAlerts()
        }
        // Auto-check for app updates silently in background on launch
        checkForUpdatesSilently()
    }

    fun checkForUpdatesSilently() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updateMgr = com.example.update.AppUpdateManager(getApplication())
                val res = updateMgr.checkForUpdates()
                if (res.isSuccess) {
                    val info = res.getOrNull()
                    if (info != null && info.hasUpdate) {
                        _availableUpdate.value = info
                    }
                }
            } catch (e: Exception) {
                // Background check fails silently without interrupting user
            }
        }
    }

    fun dismissUpdatePrompt() {
        _availableUpdate.value = null
    }

    fun publishNewVersionToCloud(
        version: String = "2.1",
        notes: String = "Versión 2.1: Continuación automática de presupuesto entre quincenas al superar el 100%, selección de origen de fondos (Presupuesto o Fondos Extraordinarios) en Gastos Programados, soporte completo para pagos recurrentes mensuales con renovación automática y corrección en la edición/eliminación de registros e inversiones.",
        apkUrl: String = "https://github.com/jfrankjfas/MyFinance/releases/download/v2.1/FinanzasClaras-v2.1.apk"
    ) {
        repository.publishVersionToCloud(version, notes, apkUrl)
        _importMessage.value = "🚀 Versión v$version publicada en Firebase Firestore (app_config/version). Todos los dispositivos la detectarán automáticamente."
    }

    fun syncToFirebase() {
        val email = _userEmail.value
        viewModelScope.launch {
            _isAiLoading.value = true
            _importMessage.value = "Sincronizando con Firebase..."
            try {
                if (email.isNotBlank()) {
                    val count = repository.syncBidirectional(email)
                    _importMessage.value = "🟢 Sincronización exitosa con Firebase ($count registros activos)."
                } else {
                    repository.syncAllToCloud()
                    _importMessage.value = "🟢 Datos locales enviados a Firebase."
                }
            } catch (e: Exception) {
                Log.e("FinanceViewModel", "Manual sync error: ${e.message}")
                _importMessage.value = "🟢 Sincronizado localmente (Firebase en espera)."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun setActiveCurrencyIndex(index: Int) {
        val safeIndex = index.coerceIn(0, 2)
        _activeCurrencyIndex.value = safeIndex
        val symbol = _favoriteCurrencies.value.getOrNull(safeIndex)?.symbol ?: "$"
        _currencySymbol.value = symbol
        appPrefs.edit()
            .putInt("active_currency_index", safeIndex)
            .putString("currency_symbol", symbol)
            .apply()
    }

    fun setCurrency(symbol: String) {
        val idx = _favoriteCurrencies.value.indexOfFirst { it.symbol == symbol }
        val safeIndex = if (idx >= 0) idx else 0
        _activeCurrencyIndex.value = safeIndex
        _currencySymbol.value = symbol
        appPrefs.edit()
            .putInt("active_currency_index", safeIndex)
            .putString("currency_symbol", symbol)
            .apply()
    }

    fun setThreeCurrencies(
        c1: CurrencyItem,
        c2: CurrencyItem,
        c3: CurrencyItem,
        customRate2: Double? = null,
        customRate3: Double? = null
    ) {
        val baseCordoba = if (c1.code == "NIO") c1 else WorldCurrencies.DEFAULT_3[0]
        _favoriteCurrencies.value = listOf(baseCordoba, c2, c3)
        val rate2 = customRate2 ?: c2.defaultRateToNio
        val rate3 = customRate3 ?: c3.defaultRateToNio
        _exchangeRate2.value = rate2
        _exchangeRate3.value = rate3
        
        // Refresh symbol according to current index
        val symbol = listOf(baseCordoba, c2, c3).getOrNull(_activeCurrencyIndex.value)?.symbol ?: baseCordoba.symbol
        _currencySymbol.value = symbol

        appPrefs.edit()
            .putString("exchange_rate_2", rate2.toString())
            .putString("exchange_rate_3", rate3.toString())
            .putString("currency_symbol", symbol)
            .apply()
    }

    fun setAppTheme(theme: String) {
        _appTheme.value = theme
    }

    fun setAppLanguage(language: String) {
        _appLanguage.value = language
    }

    fun updateExchangeRates(rate2: Double, rate3: Double) {
        if (rate2 > 0) {
            _exchangeRate2.value = rate2
            appPrefs.edit().putString("exchange_rate_2", rate2.toString()).apply()
        }
        if (rate3 > 0) {
            _exchangeRate3.value = rate3
            appPrefs.edit().putString("exchange_rate_3", rate3.toString()).apply()
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            _isAiLoading.value = true
            _importMessage.value = "Resguardando datos y cerrando sesión..."
            try {
                // Sincronizar datos pendientes antes de cerrar la sesión local
                if (_isGoogleDriveConnected.value && _userEmail.value.isNotBlank()) {
                    repository.syncAllToCloud()
                }
            } catch (e: Exception) {
                // Sync error non-fatal on logout
            }

            // Desconectar credenciales de Google Identity Services
            try {
                val googleIdManager = GoogleIdentityManager(getApplication())
                googleIdManager.signOut()
            } catch (e: Exception) {
                // Ignore
            }

            // 1. Desconectar listeners de Firebase
            firebaseManager.disconnectUser()

            // 2. Preservar registros locales en el dispositivo (NO borrar datos por seguridad)
            // repository.clearLocalDataOnly() eliminado para evitar pérdida accidental de datos

            // 3. Actualizar estado en memoria
            _userEmail.value = ""
            _userName.value = "Sin sesión"
            _isGoogleDriveConnected.value = false
            _lastDriveSync.value = "Sesión cerrada"

            // 4. Guardar preferencia para recordar que se cerró sesión y resetear moneda principal a Córdobas
            _activeCurrencyIndex.value = 0
            _currencySymbol.value = "C$"
            appPrefs.edit()
                .putBoolean("is_logged_in", false)
                .putString("saved_email", "")
                .putString("saved_name", "")
                .putInt("active_currency_index", 0)
                .putString("currency_symbol", "C$")
                .apply()

            _isAiLoading.value = false
            _importMessage.value = "🔒 Sesión cerrada. Registros retirados de la app. Tus datos están a salvo en tu cuenta Gmail."
        }
    }

    fun loginUser(
        email: String,
        name: String,
        securityPin: String? = null,
        securityManager: com.example.security.AppSecurityManager? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val displayName = if (name.isBlank()) {
            cleanEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } else {
            name.trim()
        }

        viewModelScope.launch {
            _isAiLoading.value = true
            _importMessage.value = "Conectando con cuenta Gmail ($cleanEmail)..."

            _userEmail.value = cleanEmail
            _userName.value = displayName
            _isGoogleDriveConnected.value = true

            // Guardar preferencia de sesión
            appPrefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("saved_email", cleanEmail)
                .putString("saved_name", displayName)
                .apply()

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            _lastDriveSync.value = "Conectado ahora (${sdf.format(Date())})"

            try {
                // 1. Vincular cuenta a Firebase Firestore
                firebaseManager.updateUserAccount(cleanEmail)

                // 2. Mandar a traer y sincronizar todos los registros y PIN desde la nube
                _importMessage.value = "Sincronizando y respaldando registros con tu cuenta Gmail..."
                var cloudPinFound: String? = null
                val count = repository.syncBidirectional(cleanEmail) { cloudPin ->
                    cloudPinFound = cloudPin
                    securityManager?.setPinFromCloud(cloudPin)
                }

                // Si el usuario suministró un PIN explícito al iniciar sesión, se graba en Firebase (pinuser)
                if (!securityPin.isNullOrBlank()) {
                    securityManager?.updatePin(securityPin)
                    repository.saveUserPinToCloud(cleanEmail, securityPin)
                } else if (!cloudPinFound.isNullOrBlank()) {
                    securityManager?.setPinFromCloud(cloudPinFound!!)
                }

                if (count > 0) {
                    _importMessage.value = "✅ ¡Bienvenido, $displayName! Se restauraron tus $count registros vinculados a $cleanEmail."
                } else {
                    _importMessage.value = "✅ ¡Bienvenido, $displayName! Cuenta $cleanEmail vinculada. Registros sincronizados en tiempo real."
                }
                onSuccess?.invoke()
            } catch (e: Exception) {
                Log.e("FinanceViewModel", "Login error: ${e.message}")
                _importMessage.value = "✅ ¡Bienvenido, $displayName! Sesión iniciada."
                onSuccess?.invoke()
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun updateUserSecurityPin(
        newPin: String,
        securityManager: com.example.security.AppSecurityManager?,
        onComplete: (Boolean) -> Unit
    ) {
        if (newPin.length !in 4..8) {
            onComplete(false)
            return
        }
        val email = _userEmail.value
        val updated = securityManager?.updatePin(newPin) ?: false
        if (updated && email.isNotBlank()) {
            repository.saveUserPinToCloud(email, newPin)
            _importMessage.value = "🔒 PIN de seguridad actualizado y guardado en Firebase (pinuser)"
            onComplete(true)
        } else {
            onComplete(updated)
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            val email = _userEmail.value
            if (email.isNotBlank()) {
                _isAiLoading.value = true
                _importMessage.value = "Descargando registros desde tu cuenta Gmail ($email)..."
                val count = repository.fetchAndRestoreAllFromCloud(email)
                _isAiLoading.value = false
                _importMessage.value = "✅ Sincronización exitosa: Se descargaron $count registros desde tu cuenta Gmail."
            } else {
                _importMessage.value = "⚠️ Inicia sesión con tu cuenta Google para descargar tus registros."
            }
        }
    }

    fun syncToGoogleDrive() {

        viewModelScope.launch {
            _isAiLoading.value = true
            val json = repository.exportDataToJson()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val nowStr = sdf.format(Date())
            _lastDriveSync.value = "Sincronizado hoy $nowStr en Google Drive"
            _backupJson.value = json
            _importMessage.value = "✅ Copia de seguridad guardada exitosamente en Google Drive (FinanzasClara_Backup.json)."
            _isAiLoading.value = false
        }
    }

    fun restoreFromGoogleDrive() {
        viewModelScope.launch {
            _isAiLoading.value = true
            val currentBackup = _backupJson.value
            if (!currentBackup.isNullOrEmpty()) {
                val success = repository.importDataFromJson(currentBackup)
                if (success) {
                    _importMessage.value = "✅ Datos restaurados exitosamente desde la nube de Google Drive."
                } else {
                    _importMessage.value = "❌ No se pudo restaurar el archivo desde Google Drive."
                }
            } else {
                _importMessage.value = "ℹ️ Sincronizando respaldo más reciente desde Google Drive..."
                syncToGoogleDrive()
            }
            _isAiLoading.value = false
        }
    }

    fun generateExcelCsvReport() {
        viewModelScope.launch {
            val txs = uiState.value.transactions
            val currency = uiState.value.currencySymbol
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            
            val csvBuilder = StringBuilder()
            csvBuilder.append("Fecha,Título,Categoría,Tipo,Monto,Moneda,IA Categorizado,Notas\n")
            
            txs.forEach { tx ->
                val formattedDate = sdf.format(Date(tx.timestamp))
                val titleEscaped = "\"${tx.title.replace("\"", "\"\"")}\""
                val categoryEscaped = "\"${tx.category.replace("\"", "\"\"")}\""
                val noteEscaped = "\"${tx.note.replace("\"", "\"\"")}\""
                val typeLabel = if (tx.type == "INCOME") "INGRESO" else "GASTO"
                val aiLabel = if (tx.isAiCategorized) "Sí" else "No"
                
                csvBuilder.append("$formattedDate,$titleEscaped,$categoryEscaped,$typeLabel,${tx.amount},$currency,$aiLabel,$noteEscaped\n")
            }
            
            _csvExportData.value = csvBuilder.toString()
            _importMessage.value = "✅ Reporte Excel (.csv) generado. Puedes copiarlo o guardarlo."
        }
    }

    fun clearCsvData() {
        _csvExportData.value = null
    }

    fun addTransaction(
        title: String,
        amount: Double,
        category: String,
        type: String,
        note: String = "",
        isAi: Boolean = false,
        timestamp: Long = System.currentTimeMillis(),
        attachmentUri: String? = null,
        dueDate: Long? = null,
        schedulePaymentReminder: Boolean = false,
        currency: String = "NIO"
    ) {
        viewModelScope.launch {
            val rate = if (currency == "USD") _exchangeRate2.value else 1.0
            val baseAmount = if (currency == "USD") amount * rate else amount
            val formattedNote = if (currency == "USD" && !note.contains("USD")) {
                "[$amount USD] $note".trim()
            } else {
                note
            }
            val newTx = TransactionEntity(
                title = title,
                amount = baseAmount,
                category = category,
                type = type,
                note = formattedNote,
                isAiCategorized = isAi,
                timestamp = timestamp,
                attachmentUri = attachmentUri,
                dueDate = dueDate,
                hasReminderScheduled = schedulePaymentReminder,
                originalAmount = amount,
                originalCurrency = currency,
                exchangeRate = rate
            )
            repository.addTransaction(newTx)

            // If user requested or attachment has detected due date, calendarize and schedule reminders 2 days and 1 day before
            if (schedulePaymentReminder && dueDate != null && dueDate > System.currentTimeMillis()) {
                val scheduled = ScheduledExpenseEntity(
                    title = title,
                    amount = baseAmount,
                    category = category,
                    dueDate = dueDate,
                    isPaid = false,
                    notifyReminder = true,
                    attachmentUri = attachmentUri,
                    note = formattedNote
                )
                val id = repository.addScheduledExpense(scheduled)
                val scheduledWithId = scheduled.copy(id = id)
                PaymentAlarmScheduler.schedulePaymentReminders(getApplication(), scheduledWithId, uiState.value.currencySymbol)
                _importMessage.value = "✅ Registro confirmado: $title. ⏰ Pago calendarizado y recordatorios activados."
            } else {
                _importMessage.value = "✅ Registro confirmado: $title."
            }

            checkBudgetAlertsAndNotify(category, baseAmount, type)
        }
    }

    fun editTransaction(
        originalTransaction: TransactionEntity,
        title: String,
        amount: Double,
        category: String,
        type: String,
        note: String = "",
        timestamp: Long = originalTransaction.timestamp,
        currency: String = "NIO"
    ) {
        viewModelScope.launch {
            val rate = if (currency == "USD") _exchangeRate2.value else 1.0
            val baseAmount = if (currency == "USD") amount * rate else amount
            val formattedNote = if (currency == "USD") {
                val usdTag = "[$amount USD]"
                if (!note.contains("USD")) "$usdTag $note".trim() else note
            } else {
                note.replace(Regex("""\[\d+(\.\d+)? USD\]"""), "").trim()
            }
            val updated = originalTransaction.copy(
                title = title.trim(),
                amount = baseAmount,
                originalAmount = amount,
                originalCurrency = currency,
                exchangeRate = rate,
                category = category,
                type = type,
                note = formattedNote,
                timestamp = timestamp
            )
            repository.updateTransaction(updated)
            _importMessage.value = "✏️ Registro actualizado correctamente."
            checkBudgetAlertsAndNotify(category, baseAmount, type)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _importMessage.value = "🗑️ Registro eliminado correctamente."
        }
    }

    fun saveBudgetLimit(category: String, limitAmount: Double, thresholdPercent: Int = 80) {
        viewModelScope.launch {
            val baseAmount = convertToCordobas(limitAmount)
            repository.saveBudget(
                BudgetEntity(
                    category = category,
                    limitAmount = baseAmount,
                    alertThresholdPercent = thresholdPercent
                )
            )
            val label = if (category == "GENERAL") "Presupuesto General" else category
            _importMessage.value = "✅ Presupuesto confirmado para $label."
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudgetByCategory(category)
            _importMessage.value = "🗑️ Presupuesto de $category eliminado."
        }
    }

    fun processAiInput(userPrompt: String, onResult: (AiCategorizedResult) -> Unit) {
        if (userPrompt.isBlank()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null

            val result = GeminiCategorizer.categorizeTransactionInput(userPrompt)
            _isAiLoading.value = false

            result.onSuccess { categorized ->
                onResult(categorized)
            }.onFailure { err ->
                _aiErrorMessage.value = err.message ?: "Error al procesar con Inteligencia Artificial"
            }
        }
    }

    fun clearAiError() {
        _aiErrorMessage.value = null
    }

    fun generateBackupJson() {
        viewModelScope.launch {
            val json = repository.exportDataToJson()
            _backupJson.value = json
        }
    }

    fun restoreBackupJson(jsonString: String) {
        viewModelScope.launch {
            val success = repository.importDataFromJson(jsonString)
            if (success) {
                _importMessage.value = "¡Respaldo restaurado con éxito!"
            } else {
                _importMessage.value = "Error al leer el archivo de respaldo. Verifica el formato."
            }
        }
    }

    fun clearImportMessage() {
        _importMessage.value = null
    }

    fun testBudgetNotification(categoryName: String = "Alimentación") {
        NotificationHelper.sendBudgetAlertNotification(
            context = getApplication(),
            category = categoryName,
            spentAmount = 360.0,
            limitAmount = 400.0,
            percent = 90
        )
    }

    fun setBudgetPeriodMode(mode: String) {
        _budgetPeriodMode.value = mode
    }

    fun addScheduledExpense(
        title: String,
        amount: Double,
        category: String,
        dueDate: Long,
        notifyReminder: Boolean,
        attachmentUri: String? = null,
        note: String = "",
        isEmergencyPriority: Boolean = false,
        isRecurringMonthly: Boolean = false,
        fundingSource: String = "PERIOD_BUDGET",
        extraordinaryFundId: Long? = null,
        extraordinaryFundTitle: String = ""
    ) {
        viewModelScope.launch {
            val baseAmount = convertToCordobas(amount)

            val entity = ScheduledExpenseEntity(
                title = title,
                amount = baseAmount,
                category = category,
                dueDate = dueDate,
                isPaid = false,
                notifyReminder = notifyReminder,
                attachmentUri = attachmentUri,
                note = note,
                isEmergencyPriority = isEmergencyPriority,
                isRecurringMonthly = isRecurringMonthly,
                currency = "C$",
                fundingSource = fundingSource,
                extraordinaryFundId = extraordinaryFundId,
                extraordinaryFundTitle = extraordinaryFundTitle
            )
            val id = repository.addScheduledExpense(entity)
            val entityWithId = entity.copy(id = id)

            if (notifyReminder) {
                PaymentAlarmScheduler.schedulePaymentReminders(getApplication(), entityWithId, uiState.value.currencySymbol)
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                _importMessage.value = "✅ Gasto programado confirmado: $title. ⏰ Recordatorio activado (${sdf.format(Date(dueDate))})."
            } else {
                _importMessage.value = "✅ Gasto programado confirmado: $title."
            }
        }
    }

    fun updateScheduledExpense(
        id: Long,
        title: String,
        amount: Double,
        category: String,
        dueDate: Long,
        notifyReminder: Boolean,
        attachmentUri: String? = null,
        note: String = "",
        isEmergencyPriority: Boolean = false,
        isRecurringMonthly: Boolean = false,
        fundingSource: String = "PERIOD_BUDGET",
        extraordinaryFundId: Long? = null,
        extraordinaryFundTitle: String = ""
    ) {
        viewModelScope.launch {
            val baseAmount = convertToCordobas(amount)
            val entity = ScheduledExpenseEntity(
                id = id,
                title = title,
                amount = baseAmount,
                category = category,
                dueDate = dueDate,
                isPaid = false,
                notifyReminder = notifyReminder,
                attachmentUri = attachmentUri,
                note = note,
                isEmergencyPriority = isEmergencyPriority,
                isRecurringMonthly = isRecurringMonthly,
                currency = "C$",
                fundingSource = fundingSource,
                extraordinaryFundId = extraordinaryFundId,
                extraordinaryFundTitle = extraordinaryFundTitle
            )
            repository.updateScheduledExpense(entity)
            if (notifyReminder) {
                PaymentAlarmScheduler.schedulePaymentReminders(getApplication(), entity, uiState.value.currencySymbol)
            } else {
                PaymentAlarmScheduler.cancelPaymentReminders(getApplication(), id)
            }
            _importMessage.value = "✅ Gasto programado actualizado: $title."
        }
    }

    fun markScheduledExpenseAsPaid(
        expense: ScheduledExpenseEntity,
        chosenFundingSource: String? = null,
        chosenExtraordinaryFundId: Long? = null,
        chosenExtraordinaryFundTitle: String? = null
    ) {
        viewModelScope.launch {
            val effectiveFundingSource = chosenFundingSource ?: expense.fundingSource
            val effectiveFundId = chosenExtraordinaryFundId ?: expense.extraordinaryFundId
            val effectiveFundTitle = chosenExtraordinaryFundTitle ?: expense.extraordinaryFundTitle

            PaymentAlarmScheduler.cancelPaymentReminders(getApplication(), expense.id)
            repository.updateScheduledExpense(
                expense.copy(
                    isPaid = true,
                    fundingSource = effectiveFundingSource,
                    extraordinaryFundId = effectiveFundId,
                    extraordinaryFundTitle = effectiveFundTitle
                )
            )

            // If paid from an Extraordinary Fund, register the allocation in that fund to discount its balance
            if (effectiveFundingSource == "EXTRAORDINARY_FUND" && effectiveFundId != null && effectiveFundId > 0) {
                addAllocationToExtraordinaryFund(
                    fundId = effectiveFundId,
                    allocationTitle = "Pago: ${expense.title}",
                    amount = expense.amount, // base amount in cordobas
                    category = expense.category,
                    note = "Gasto programado liquidado desde este fondo"
                )
            }

            // Create a real transaction for this paid scheduled expense
            val sourceTag = when (effectiveFundingSource) {
                "EXTRAORDINARY_FUND" -> " [Pagado de Fondo Extraordinario: $effectiveFundTitle]"
                "SAVINGS" -> " [Pagado con Ahorros / Fondos Externos]"
                else -> " [Presupuesto del Período]"
            }
            val newTx = TransactionEntity(
                title = "Pago: ${expense.title}",
                amount = expense.amount,
                category = expense.category,
                type = "EXPENSE",
                timestamp = System.currentTimeMillis(),
                note = "Gasto programado registrado como pagado$sourceTag" + if (expense.note.isNotBlank()) " (${expense.note})" else "",
                attachmentUri = expense.attachmentUri
            )
            repository.addTransaction(newTx)

            // If it's recurring monthly, schedule next month's payment!
            if (expense.isRecurringMonthly) {
                val nextCal = Calendar.getInstance().apply {
                    timeInMillis = expense.dueDate
                    add(Calendar.MONTH, 1)
                }
                val nextExpense = ScheduledExpenseEntity(
                    title = expense.title,
                    amount = expense.amount,
                    category = expense.category,
                    dueDate = nextCal.timeInMillis,
                    isPaid = false,
                    notifyReminder = expense.notifyReminder,
                    attachmentUri = expense.attachmentUri,
                    note = expense.note,
                    isEmergencyPriority = expense.isEmergencyPriority,
                    isRecurringMonthly = true,
                    currency = expense.currency,
                    fundingSource = effectiveFundingSource,
                    extraordinaryFundId = effectiveFundId,
                    extraordinaryFundTitle = effectiveFundTitle
                )
                val nextId = repository.addScheduledExpense(nextExpense)
                if (nextExpense.notifyReminder) {
                    PaymentAlarmScheduler.schedulePaymentReminders(getApplication(), nextExpense.copy(id = nextId), uiState.value.currencySymbol)
                }
            }

            NotificationHelper.sendBudgetAlertNotification(
                context = getApplication(),
                category = expense.category,
                spentAmount = expense.amount,
                limitAmount = expense.amount,
                percent = 100
            )
            _importMessage.value = "✅ Pago registrado con éxito (${when(effectiveFundingSource) {
                "EXTRAORDINARY_FUND" -> "descontado de Fondo Extraordinario: $effectiveFundTitle"
                "SAVINGS" -> "cubierto con Ahorros/Otros Recursos"
                else -> "cargado al Presupuesto del Período"
            }})." + if (expense.isRecurringMonthly) " 🔁 Siguiente pago programado para el próximo mes." else ""
        }
    }

    fun deleteScheduledExpense(expense: ScheduledExpenseEntity) {
        viewModelScope.launch {
            PaymentAlarmScheduler.cancelPaymentReminders(getApplication(), expense.id)
            repository.deleteScheduledExpense(expense)
            _importMessage.value = "🗑️ Gasto programado '${expense.title}' eliminado."
        }
    }

    fun clearAllDataToZero() {
        viewModelScope.launch {
            _isAiLoading.value = true
            repository.clearAllDatabaseAndCloud(getApplication())
            _importMessage.value = "🧹 ¡Base de datos reiniciada a cero! Listo para comenzar ordenado y sin deudas atrasadas."
            _isAiLoading.value = false
        }
    }

    fun scanReceiptWithDueDate(uri: android.net.Uri, onResult: (ReceiptScanResult) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiErrorMessage.value = null
            val result = GeminiCategorizer.scanReceiptImageWithDueDate(getApplication(), uri)
            _isAiLoading.value = false
            result.onSuccess {
                onResult(it)
            }.onFailure {
                _aiErrorMessage.value = "Error al escanear comprobante: ${it.message}"
            }
        }
    }

    fun checkUpcomingPaymentAlerts() {
        viewModelScope.launch {
            val scheduled = uiState.value.scheduledExpenses.filter { !it.isPaid && it.notifyReminder }
            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            scheduled.forEach { expense ->
                val diffMs = expense.dueDate - now
                val daysRemaining = (diffMs / dayMs).toInt()
                val dueDateFormatted = sdf.format(Date(expense.dueDate))
                val amountFormatted = "${uiState.value.currencySymbol}${String.format(Locale.US, "%.2f", expense.amount)}"

                if (diffMs > 0 && daysRemaining == 2) {
                    NotificationHelper.sendPaymentDueReminder(
                        getApplication(),
                        expense.title,
                        amountFormatted,
                        2,
                        dueDateFormatted
                    )
                } else if (diffMs > 0 && daysRemaining == 1) {
                    NotificationHelper.sendPaymentDueReminder(
                        getApplication(),
                        expense.title,
                        amountFormatted,
                        1,
                        dueDateFormatted
                    )
                }
            }
        }
    }

    fun testScheduledExpenseReminder(title: String = "Servicio de Luz", amount: Double = 45.0): Boolean {
        val sent = NotificationHelper.sendTestNotification(
            context = getApplication(),
            currencySymbol = uiState.value.currencySymbol
        )
        if (sent) {
            _importMessage.value = "🔔 ¡Notificación de prueba enviada con éxito! Revisa la barra de notificaciones de tu teléfono."
        } else {
            _importMessage.value = "⚠️ No se pudo mostrar la notificación. Asegúrate de conceder el permiso de notificaciones en los Ajustes de la aplicación."
        }
        return sent
    }

    private fun isSameMonthAndYear(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    fun archiveAndClosePeriod(
        customTitle: String = "",
        note: String = "",
        periodMode: String = uiState.value.budgetPeriodMode,
        startTimestamp: Long? = null,
        endTimestamp: Long? = null,
        clearPeriodData: Boolean = true,
        closePermanently: Boolean = true
    ) {
        viewModelScope.launch {
            val state = uiState.value
            val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            val monthStr = sdfMonth.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

            val periodLabel = when (periodMode) {
                "FORTNIGHT_1" -> "1ª Quincena ($monthStr)"
                "FORTNIGHT_2" -> "2ª Quincena ($monthStr)"
                "CUSTOM" -> if (startTimestamp != null && endTimestamp != null) {
                    val sdfShort = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    "${sdfShort.format(Date(startTimestamp))} - ${sdfShort.format(Date(endTimestamp))}"
                } else "Periodo Personalizado"
                else -> "Mes de $monthStr"
            }

            val finalTitle = customTitle.ifBlank { "Estado de Cuenta - $periodLabel" }

            val periodTxs = state.transactions.filter { tx ->
                if (startTimestamp != null && endTimestamp != null) {
                    tx.timestamp in startTimestamp..endTimestamp
                } else {
                    isTimestampInPeriod(tx.timestamp, periodMode)
                }
            }

            val txArray = JSONArray()
            var spentSum = 0.0
            periodTxs.forEach { tx ->
                val obj = JSONObject().apply {
                    put("title", tx.title)
                    put("amount", tx.amount)
                    put("category", tx.category)
                    put("type", tx.type)
                    put("timestamp", tx.timestamp)
                    put("note", tx.note)
                }
                txArray.put(obj)
                if (tx.type == "EXPENSE") {
                    spentSum += tx.amount
                }
            }

            val schedArray = JSONArray()
            val periodScheduled = state.scheduledExpenses.filter { s ->
                if (startTimestamp != null && endTimestamp != null) {
                    s.dueDate in startTimestamp..endTimestamp
                } else {
                    isTimestampInPeriod(s.dueDate, periodMode)
                }
            }
            var schedSum = 0.0
            periodScheduled.forEach { s ->
                val obj = JSONObject().apply {
                    put("title", s.title)
                    put("amount", s.amount)
                    put("category", s.category)
                    put("dueDate", s.dueDate)
                    put("isPaid", s.isPaid)
                }
                schedArray.put(obj)
                if (!s.isPaid) {
                    schedSum += s.amount
                }
            }

            val entity = ArchivedPeriodEntity(
                id = 0,
                title = finalTitle,
                periodMode = periodMode,
                archivedAt = System.currentTimeMillis(),
                budgetLimit = state.periodBudgetLimitTotal,
                totalSpent = spentSum,
                totalScheduled = schedSum,
                currencySymbol = state.currencySymbol,
                note = note,
                transactionsJson = txArray.toString(),
                scheduledJson = schedArray.toString(),
                isClosed = closePermanently
            )

            repository.archivePeriod(entity)

            if (clearPeriodData) {
                periodTxs.forEach { repository.deleteTransaction(it) }
                periodScheduled.forEach { repository.deleteScheduledExpense(it) }
                _importMessage.value = if (closePermanently) {
                    "🔒 ¡Estado de Cuenta cerrado oficialmente! El periodo ha quedado sellado sin permitir cambios y los registros activos se reiniciaron a cero."
                } else {
                    "✅ ¡Periodo respaldado y limpiado! Transacciones guardadas en Estado de Cuenta."
                }
            } else {
                _importMessage.value = if (closePermanently) {
                    "🔒 ¡Estado de Cuenta cerrado oficialmente! Ha quedado sellado como documento inmutable."
                } else {
                    "✅ ¡Periodo respaldado con éxito! Se guardó tu Estado de Cuenta en el Archivo Histórico."
                }
            }
        }
    }

    fun archiveCurrentPeriod(customTitle: String = "", note: String = "", clearPeriodData: Boolean = true) {
        viewModelScope.launch {
            val state = uiState.value
            val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
            val monthStr = sdfMonth.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

            val periodLabel = when (state.budgetPeriodMode) {
                "FORTNIGHT_1" -> "1ª Quincena ($monthStr)"
                "FORTNIGHT_2" -> "2ª Quincena ($monthStr)"
                else -> "Mes de $monthStr"
            }

            val finalTitle = customTitle.ifBlank { "Estado de Cuenta - $periodLabel" }

            val periodTxs = state.transactions.filter { isTimestampInPeriod(it.timestamp, state.budgetPeriodMode) }
            val txArray = JSONArray()
            periodTxs.forEach { tx ->
                val obj = JSONObject().apply {
                    put("title", tx.title)
                    put("amount", tx.amount)
                    put("category", tx.category)
                    put("type", tx.type)
                    put("timestamp", tx.timestamp)
                    put("note", tx.note)
                }
                txArray.put(obj)
            }

            val schedArray = JSONArray()
            state.periodScheduledExpenses.forEach { s ->
                val obj = JSONObject().apply {
                    put("title", s.title)
                    put("amount", s.amount)
                    put("category", s.category)
                    put("dueDate", s.dueDate)
                    put("isPaid", s.isPaid)
                }
                schedArray.put(obj)
            }

            val existingPeriod = state.archivedPeriods.firstOrNull { 
                it.periodMode == state.budgetPeriodMode && isSameMonthAndYear(it.archivedAt, System.currentTimeMillis())
            }

            val entity = ArchivedPeriodEntity(
                id = existingPeriod?.id ?: 0,
                title = finalTitle,
                periodMode = state.budgetPeriodMode,
                archivedAt = System.currentTimeMillis(),
                budgetLimit = state.periodBudgetLimitTotal,
                totalSpent = state.periodSpentTotal,
                totalScheduled = state.periodPendingExpensesTotal,
                currencySymbol = state.currencySymbol,
                note = note,
                transactionsJson = txArray.toString(),
                scheduledJson = schedArray.toString()
            )

            repository.archivePeriod(entity)

            if (clearPeriodData) {
                // Remove transactions belonging to the archived period from the active database
                periodTxs.forEach { repository.deleteTransaction(it) }
                // Remove scheduled expenses of this period from active list
                state.periodScheduledExpenses.forEach { repository.deleteScheduledExpense(it) }
                _importMessage.value = "✅ ¡Periodo respaldado y limpiado! Transacciones guardadas en Estado de Cuenta. Nuevo periodo listo en cero."
            } else {
                _importMessage.value = "✅ ¡Periodo respaldado con éxito! Se guardó tu Estado de Cuenta en el Archivo Histórico."
            }
        }
    }

    fun deleteArchivedPeriod(period: ArchivedPeriodEntity) {
        viewModelScope.launch {
            repository.deleteArchivedPeriod(period)
            _importMessage.value = "🗑️ Estado de Cuenta eliminado del archivo."
        }
    }

    fun closeArchivedPeriod(periodId: Long) {
        viewModelScope.launch {
            val period = uiState.value.archivedPeriods.find { it.id == periodId } ?: return@launch
            val updated = period.copy(isClosed = true)
            repository.updateArchivedPeriod(updated)
            _importMessage.value = "🔒 ¡Estado de Cuenta cerrado oficialmente! Ha quedado en modo de solo lectura."
        }
    }

    fun addTransactionToArchivedPeriod(periodId: Long, title: String, amount: Double, category: String, type: String, note: String) {
        viewModelScope.launch {
            val period = uiState.value.archivedPeriods.find { it.id == periodId } ?: return@launch
            if (period.isClosed) return@launch

            val jsonArray = try { JSONArray(period.transactionsJson) } catch (e: Exception) { JSONArray() }
            val newObj = JSONObject().apply {
                put("title", title)
                put("amount", amount)
                put("category", category)
                put("type", type)
                put("timestamp", System.currentTimeMillis())
                put("note", note)
            }
            jsonArray.put(newObj)

            // Recalculate total spent
            var newTotalSpent = 0.0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("type", "EXPENSE") == "EXPENSE") {
                    newTotalSpent += obj.optDouble("amount", 0.0)
                }
            }

            val updated = period.copy(
                transactionsJson = jsonArray.toString(),
                totalSpent = newTotalSpent
            )
            repository.updateArchivedPeriod(updated)
            _importMessage.value = "✏️ Movimiento agregado al Estado de Cuenta."
        }
    }

    fun editTransactionInArchivedPeriod(periodId: Long, txIndex: Int, title: String, amount: Double, category: String, type: String, note: String) {
        viewModelScope.launch {
            val period = uiState.value.archivedPeriods.find { it.id == periodId } ?: return@launch
            if (period.isClosed) return@launch

            val jsonArray = try { JSONArray(period.transactionsJson) } catch (e: Exception) { JSONArray() }
            if (txIndex < 0 || txIndex >= jsonArray.length()) return@launch

            val updatedObj = JSONObject().apply {
                put("title", title)
                put("amount", amount)
                put("category", category)
                put("type", type)
                put("timestamp", jsonArray.getJSONObject(txIndex).optLong("timestamp", System.currentTimeMillis()))
                put("note", note)
            }
            jsonArray.put(txIndex, updatedObj)

            // Recalculate total spent
            var newTotalSpent = 0.0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("type", "EXPENSE") == "EXPENSE") {
                    newTotalSpent += obj.optDouble("amount", 0.0)
                }
            }

            val updated = period.copy(
                transactionsJson = jsonArray.toString(),
                totalSpent = newTotalSpent
            )
            repository.updateArchivedPeriod(updated)
            _importMessage.value = "✏️ Movimiento actualizado en Estado de Cuenta."
        }
    }

    fun deleteTransactionFromArchivedPeriod(periodId: Long, txIndex: Int) {
        viewModelScope.launch {
            val period = uiState.value.archivedPeriods.find { it.id == periodId } ?: return@launch
            if (period.isClosed) return@launch

            val jsonArray = try { JSONArray(period.transactionsJson) } catch (e: Exception) { JSONArray() }
            if (txIndex < 0 || txIndex >= jsonArray.length()) return@launch

            val newArray = JSONArray()
            for (i in 0 until jsonArray.length()) {
                if (i != txIndex) {
                    newArray.put(jsonArray.get(i))
                }
            }

            // Recalculate total spent
            var newTotalSpent = 0.0
            for (i in 0 until newArray.length()) {
                val obj = newArray.getJSONObject(i)
                if (obj.optString("type", "EXPENSE") == "EXPENSE") {
                    newTotalSpent += obj.optDouble("amount", 0.0)
                }
            }

            val updated = period.copy(
                transactionsJson = newArray.toString(),
                totalSpent = newTotalSpent
            )
            repository.updateArchivedPeriod(updated)
            _importMessage.value = "🗑️ Movimiento eliminado del Estado de Cuenta."
        }
    }

    // --- EXTRAORDINARY FUNDS MANAGEMENT ---

    fun addExtraordinaryFund(title: String, totalAmount: Double, note: String) {
        viewModelScope.launch {
            val baseAmount = convertToCordobas(totalAmount)
            val fund = com.example.data.entity.ExtraordinaryFundEntity(
                title = title,
                totalAmount = baseAmount,
                currencySymbol = "C$",
                createdAt = System.currentTimeMillis(),
                note = note
            )
            repository.addExtraordinaryFund(fund)
            _importMessage.value = "✅ Ingreso Extraordinario confirmado: $title."
        }
    }

    fun updateExtraordinaryFund(fund: com.example.data.entity.ExtraordinaryFundEntity) {
        viewModelScope.launch {
            val baseAmount = convertToCordobas(fund.totalAmount)
            val allFunds = repository.allExtraordinaryFunds.first()
            val existing = allFunds.find { it.id == fund.id }
            val updated = if (existing != null) {
                existing.copy(
                    title = fund.title,
                    totalAmount = baseAmount,
                    currencySymbol = "C$",
                    note = fund.note
                )
            } else {
                fund.copy(
                    totalAmount = baseAmount,
                    currencySymbol = "C$"
                )
            }
            repository.updateExtraordinaryFund(updated)
            _importMessage.value = "✅ Ingreso Extraordinario actualizado: ${fund.title}."
        }
    }

    fun deleteExtraordinaryFund(fund: com.example.data.entity.ExtraordinaryFundEntity) {
        viewModelScope.launch {
            repository.deleteExtraordinaryFund(fund)
            _importMessage.value = "🗑️ Ingreso Extraordinario eliminado."
        }
    }

    fun addAllocationToExtraordinaryFund(fundId: Long, allocationTitle: String, amount: Double, category: String, note: String) {
        viewModelScope.launch {
            val allFunds = repository.allExtraordinaryFunds.first()
            val fund = allFunds.find { it.id == fundId } ?: return@launch
            val jsonArray = try { JSONArray(fund.allocationsJson) } catch (e: Exception) { JSONArray() }
            val baseAmount = convertToCordobas(amount)

            val newObj = JSONObject().apply {
                put("id", java.util.UUID.randomUUID().toString())
                put("title", allocationTitle)
                put("amount", baseAmount)
                put("category", category)
                put("note", note)
                put("timestamp", System.currentTimeMillis())
            }
            jsonArray.put(newObj)

            val updated = fund.copy(allocationsJson = jsonArray.toString())
            repository.updateExtraordinaryFund(updated)
            _importMessage.value = "✅ Asignación confirmada: $allocationTitle."
        }
    }

    fun editAllocationInExtraordinaryFund(fundId: Long, allocationId: String, title: String, amount: Double, category: String, note: String) {
        viewModelScope.launch {
            val allFunds = repository.allExtraordinaryFunds.first()
            val fund = allFunds.find { it.id == fundId } ?: return@launch
            val jsonArray = try { JSONArray(fund.allocationsJson) } catch (e: Exception) { JSONArray() }
            val baseAmount = convertToCordobas(amount)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("id") == allocationId) {
                    obj.put("title", title)
                    obj.put("amount", baseAmount)
                    obj.put("category", category)
                    obj.put("note", note)
                    break
                }
            }

            val updated = fund.copy(allocationsJson = jsonArray.toString())
            repository.updateExtraordinaryFund(updated)
            _importMessage.value = "✏️ Asignación actualizada."
        }
    }

    fun deleteAllocationFromExtraordinaryFund(fundId: Long, allocationId: String) {
        viewModelScope.launch {
            val allFunds = repository.allExtraordinaryFunds.first()
            val fund = allFunds.find { it.id == fundId } ?: return@launch
            val jsonArray = try { JSONArray(fund.allocationsJson) } catch (e: Exception) { JSONArray() }

            val newArray = JSONArray()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("id") != allocationId) {
                    newArray.put(obj)
                }
            }

            val updated = fund.copy(allocationsJson = newArray.toString())
            repository.updateExtraordinaryFund(updated)
            _importMessage.value = "🗑️ Asignación eliminada."
        }
    }

    private suspend fun checkBudgetAlertsAndNotify(category: String, addedAmount: Double, type: String) {
        if (type != "EXPENSE") return

        val state = uiState.value
        val budget = state.budgets.find { it.category == category } ?: state.budgets.find { it.category == "GENERAL" }
        if (budget != null && budget.limitAmount > 0) {
            val currentCategorySpent = (state.categoryExpenses.find { it.category == category }?.totalAmount ?: 0.0) + addedAmount
            val pct = ((currentCategorySpent / budget.limitAmount) * 100).toInt()
            if (pct >= budget.alertThresholdPercent) {
                NotificationHelper.sendBudgetAlertNotification(
                    context = getApplication(),
                    category = budget.category,
                    spentAmount = currentCategorySpent,
                    limitAmount = budget.limitAmount,
                    percent = pct
                )
            }
        }
    }
}
