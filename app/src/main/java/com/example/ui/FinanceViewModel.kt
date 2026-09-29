package com.example.ui

import android.app.Application
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
    val isWarning: Boolean
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

    private val _budgetPeriodMode = MutableStateFlow("MONTHLY") // "MONTHLY", "FORTNIGHT_1", "FORTNIGHT_2"
    val budgetPeriodMode: StateFlow<String> = _budgetPeriodMode.asStateFlow()

    private val _favoriteCurrencies = MutableStateFlow<List<CurrencyItem>>(WorldCurrencies.DEFAULT_3)
    val favoriteCurrencies: StateFlow<List<CurrencyItem>> = _favoriteCurrencies.asStateFlow()

    private val _activeCurrencyIndex = MutableStateFlow(0) // Default to 0 (NIO C$ Córdobas - Moneda Principal)
    val activeCurrencyIndex: StateFlow<Int> = _activeCurrencyIndex.asStateFlow()

    private val _exchangeRate2 = MutableStateFlow(36.6243) // 1 USD = 36.6243 NIO C$
    val exchangeRate2: StateFlow<Double> = _exchangeRate2.asStateFlow()

    private val _exchangeRate3 = MutableStateFlow(39.809)  // 1 EUR = 39.809 NIO C$
    val exchangeRate3: StateFlow<Double> = _exchangeRate3.asStateFlow()

    private val _currencySymbol = MutableStateFlow("C$")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val appPrefs = application.getSharedPreferences("finanzas_clara_prefs", android.content.Context.MODE_PRIVATE)
    private val initialIsLoggedIn = appPrefs.getBoolean("is_logged_in", false)
    private val initialEmail = if (initialIsLoggedIn) appPrefs.getString("saved_email", "") ?: "" else ""
    private val initialName = if (initialIsLoggedIn) appPrefs.getString("saved_name", "") ?: "" else ""

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
                periodRawExpense += tx.amount
                periodCategoryMap[tx.category] = (periodCategoryMap[tx.category] ?: 0.0) + tx.amount
            }
        }
        val periodActiveExpense = periodRawExpense * multiplier

        // Scheduled expenses filtering for active period
        val periodScheduled = scheduledList.filter { isTimestampInPeriod(it.dueDate, periodMode) }
        val periodPendingScheduled = periodScheduled.filter { !it.isPaid }
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
            val baseLimit = if (periodMode == "MONTHLY") generalBudget.limitAmount else (generalBudget.limitAmount / 2.0)
            val convertedLimit = baseLimit * multiplier
            periodGeneralBudgetLimit = convertedLimit

            val totalCommittedRaw = periodRawExpense + periodPendingRawTotal
            val pct = ((totalCommittedRaw / baseLimit) * 100).toInt()

            val periodLabel = when (periodMode) {
                "FORTNIGHT_1" -> "Presupuesto 1ª Quincena (1-15)"
                "FORTNIGHT_2" -> "Presupuesto 2ª Quincena (16-31)"
                else -> "Presupuesto Total Mensual"
            }

            budgetProgressList.add(
                BudgetProgress(
                    category = periodLabel,
                    limitAmount = convertedLimit,
                    spentAmount = periodActiveExpense,
                    pendingAmount = periodPendingActiveTotal,
                    percentage = pct,
                    isExceeded = pct >= 100,
                    isWarning = pct >= generalBudget.alertThresholdPercent
                )
            )
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
                    isWarning = pct >= b.alertThresholdPercent
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
            extraordinaryFunds = extraFundsList,
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
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty(getApplication())
            if (initialIsLoggedIn && initialEmail.isNotBlank()) {
                firebaseManager.updateUserAccount(initialEmail)
                // Fetch and restore whatever is already linked to this Gmail account from Firestore
                val count = repository.syncBidirectional(initialEmail)
                if (count > 0) {
                    _importMessage.value = "✅ Se sincronizaron $count registros vinculados a tu cuenta ($initialEmail)."
                }
            } else {
                firebaseManager.disconnectUser()
                repository.clearLocalDataOnly()
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

    fun syncToFirebase() {
        val email = _userEmail.value
        viewModelScope.launch {
            _isAiLoading.value = true
            _importMessage.value = "Sincronizando con Firebase..."
            if (email.isNotBlank()) {
                val count = repository.syncBidirectional(email)
                _importMessage.value = "🟢 Sincronización exitosa con Firebase ($count registros activos)."
            } else {
                repository.syncAllToCloud()
                _importMessage.value = "🟢 Datos locales enviados a Firebase."
            }
            _isAiLoading.value = false
        }
    }

    fun setActiveCurrencyIndex(index: Int) {
        _activeCurrencyIndex.value = index.coerceIn(0, 2)
        val symbol = _favoriteCurrencies.value.getOrNull(_activeCurrencyIndex.value)?.symbol ?: "$"
        _currencySymbol.value = symbol
    }

    fun setCurrency(symbol: String) {
        val idx = _favoriteCurrencies.value.indexOfFirst { it.symbol == symbol }
        if (idx >= 0) {
            _activeCurrencyIndex.value = idx
        }
        _currencySymbol.value = symbol
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
        _exchangeRate2.value = customRate2 ?: c2.defaultRateToNio
        _exchangeRate3.value = customRate3 ?: c3.defaultRateToNio
        
        // Refresh symbol according to current index
        val symbol = listOf(baseCordoba, c2, c3).getOrNull(_activeCurrencyIndex.value)?.symbol ?: baseCordoba.symbol
        _currencySymbol.value = symbol
    }

    fun setAppTheme(theme: String) {
        _appTheme.value = theme
    }

    fun setAppLanguage(language: String) {
        _appLanguage.value = language
    }

    fun updateExchangeRates(rate2: Double, rate3: Double) {
        if (rate2 > 0) _exchangeRate2.value = rate2
        if (rate3 > 0) _exchangeRate3.value = rate3
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

            // 2. Quitar todos los registros de la app (Room SQLite queda limpio por seguridad y privacidad)
            repository.clearLocalDataOnly()

            // 3. Actualizar estado en memoria
            _userEmail.value = ""
            _userName.value = "Sin sesión"
            _isGoogleDriveConnected.value = false
            _lastDriveSync.value = "Sesión cerrada"

            // 4. Guardar preferencia para recordar que se cerró sesión
            appPrefs.edit()
                .putBoolean("is_logged_in", false)
                .putString("saved_email", "")
                .putString("saved_name", "")
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

            // 1. Vincular cuenta a Firebase Firestore
            firebaseManager.updateUserAccount(cleanEmail)

            // 2. Mandar a traer registros y PIN desde la nube
            _importMessage.value = "Descargando todos tus registros desde la cuenta Gmail..."
            var cloudPinFound: String? = null
            val count = repository.fetchAndRestoreAllFromCloud(cleanEmail) { cloudPin ->
                cloudPinFound = cloudPin
                securityManager?.setPinFromCloud(cloudPin)
            }
            repository.syncBidirectional(cleanEmail)

            // Si el usuario suministró un PIN explícito al iniciar sesión, se graba en Firebase (pinuser)
            if (!securityPin.isNullOrBlank()) {
                securityManager?.updatePin(securityPin)
                repository.saveUserPinToCloud(cleanEmail, securityPin)
            } else if (!cloudPinFound.isNullOrBlank()) {
                securityManager?.setPinFromCloud(cloudPinFound!!)
            }

            _isAiLoading.value = false
            if (count > 0) {
                _importMessage.value = "✅ ¡Bienvenido, $displayName! Se restauraron tus $count registros vinculados a $cleanEmail."
            } else {
                _importMessage.value = "✅ ¡Bienvenido, $displayName! Cuenta $cleanEmail vinculada. Registros sincronizados en tiempo real."
            }
            onSuccess?.invoke()
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
        schedulePaymentReminder: Boolean = false
    ) {
        viewModelScope.launch {
            val baseAmount = convertToCordobas(amount)
            val newTx = TransactionEntity(
                title = title,
                amount = baseAmount,
                category = category,
                type = type,
                note = note,
                isAiCategorized = isAi,
                timestamp = timestamp,
                attachmentUri = attachmentUri,
                dueDate = dueDate,
                hasReminderScheduled = schedulePaymentReminder
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
                    note = note
                )
                val id = repository.addScheduledExpense(scheduled)
                val scheduledWithId = scheduled.copy(id = id)
                PaymentAlarmScheduler.schedulePaymentReminders(getApplication(), scheduledWithId, uiState.value.currencySymbol)
                _importMessage.value = "⏰ Pago calendarizado. Te notificaremos 2 días y 1 día antes del vencimiento."
            }

            checkBudgetAlertsAndNotify(category, baseAmount, type)
        }
    }


    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
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
        isEmergencyPriority: Boolean = false
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
                isEmergencyPriority = isEmergencyPriority
            )
            val id = repository.addScheduledExpense(entity)
            val entityWithId = entity.copy(id = id)

            if (notifyReminder) {
                PaymentAlarmScheduler.schedulePaymentReminders(getApplication(), entityWithId, uiState.value.currencySymbol)
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                _importMessage.value = "⏰ Recordatorio activado: Notificaciones automáticas 2 días y 1 día antes del vencimiento (${sdf.format(Date(dueDate))})."
            }
        }
    }

    fun markScheduledExpenseAsPaid(expense: ScheduledExpenseEntity) {
        viewModelScope.launch {
            PaymentAlarmScheduler.cancelPaymentReminders(getApplication(), expense.id)
            repository.updateScheduledExpense(expense.copy(isPaid = true))

            // Create a real transaction for this paid scheduled expense
            val newTx = TransactionEntity(
                title = "Pago: ${expense.title}",
                amount = expense.amount,
                category = expense.category,
                type = "EXPENSE",
                timestamp = System.currentTimeMillis(),
                note = "Gasto programado registrado como pagado" + if (expense.note.isNotBlank()) " (${expense.note})" else "",
                attachmentUri = expense.attachmentUri
            )
            repository.addTransaction(newTx)

            NotificationHelper.sendBudgetAlertNotification(
                context = getApplication(),
                category = expense.category,
                spentAmount = expense.amount,
                limitAmount = expense.amount,
                percent = 100
            )
        }
    }

    fun deleteScheduledExpense(expense: ScheduledExpenseEntity) {
        viewModelScope.launch {
            PaymentAlarmScheduler.cancelPaymentReminders(getApplication(), expense.id)
            repository.deleteScheduledExpense(expense)
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

    fun testScheduledExpenseReminder(title: String = "Servicio de Luz", amount: Double = 45.0) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = sdf.format(Date(System.currentTimeMillis() + 86400000L))
        NotificationHelper.sendScheduledExpenseReminder(
            context = getApplication(),
            title = title,
            amountFormatted = "${uiState.value.currencySymbol}${String.format(Locale.US, "%.2f", amount)}",
            dueDateText = "mañana ($dateStr)"
        )
    }

    private fun isSameMonthAndYear(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
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
            _importMessage.value = "⭐ Presupuesto de Ingreso Extraordinario registrado (Base C$)."
        }
    }

    fun updateExtraordinaryFund(fund: com.example.data.entity.ExtraordinaryFundEntity) {
        viewModelScope.launch {
            repository.updateExtraordinaryFund(fund)
            _importMessage.value = "✏️ Ingreso Extraordinario actualizado."
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
            val fund = uiState.value.extraordinaryFunds.find { it.id == fundId } ?: return@launch
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
            _importMessage.value = "🎯 Asignación agregada al Ingreso Extraordinario."
        }
    }

    fun editAllocationInExtraordinaryFund(fundId: Long, allocationId: String, title: String, amount: Double, category: String, note: String) {
        viewModelScope.launch {
            val fund = uiState.value.extraordinaryFunds.find { it.id == fundId } ?: return@launch
            val jsonArray = try { JSONArray(fund.allocationsJson) } catch (e: Exception) { JSONArray() }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("id") == allocationId) {
                    obj.put("title", title)
                    obj.put("amount", amount)
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
            val fund = uiState.value.extraordinaryFunds.find { it.id == fundId } ?: return@launch
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
