package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.security.AppSecurityManager
import com.example.ui.FinanceViewModel
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.SecurityLockScreen
import com.example.update.AppUpdateManager
import kotlinx.coroutines.launch
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.FinanzasClaraTheme
import com.example.ui.theme.PrimaryEmerald

enum class NavTab(val title: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Default.Home),
    ANALYTICS("Análisis", Icons.Default.PieChart),
    BUDGET("Presupuesto", Icons.Default.AccountBalance),
    BACKUP("Respaldo", Icons.Default.Cloud)
}

class MainActivity : FragmentActivity() {

    private lateinit var securityManager: AppSecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        securityManager = AppSecurityManager(this)
        enableEdgeToEdge()
        setContent {
            val viewModel: FinanceViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isLocked by securityManager.isLocked.collectAsStateWithLifecycle()

            val isDark = when (uiState.appTheme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            FinanzasClaraTheme(darkTheme = isDark) {
                if (!uiState.isLoggedIn || uiState.userEmail.isBlank()) {
                    com.example.ui.screens.LoginScreen(
                        uiState = uiState,
                        securityManager = securityManager,
                        onLogin = { email, name, pin ->
                            viewModel.loginUser(email, name, pin, securityManager) {
                                securityManager.unlock()
                            }
                        }
                    )
                } else if (isLocked) {
                    SecurityLockScreen(
                        securityManager = securityManager,
                        onUnlocked = { securityManager.unlock() }
                    )
                } else {
                    MainAppScreen(
                        viewModel = viewModel,
                        securityManager = securityManager
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Re-lock app when sent to background if security is enabled
        if (::securityManager.isInitialized) {
            securityManager.lock()
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: FinanceViewModel = viewModel(),
    securityManager: AppSecurityManager? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiErrorMessage by viewModel.aiErrorMessage.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = !showProfileScreen && selectedTabIndex == index,
                        onClick = {
                            showProfileScreen = false
                            selectedTabIndex = index
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = PrimaryEmerald.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        if (showProfileScreen) {
            ProfileScreen(
                uiState = uiState,
                onLogout = { viewModel.logoutUser() },
                onLogin = { email, name -> viewModel.loginUser(email, name, securityManager = securityManager) },
                onRestoreFromCloud = { viewModel.restoreFromCloud() },
                onSetTheme = { viewModel.setAppTheme(it) },
                onSetLanguage = { viewModel.setAppLanguage(it) },
                onBack = { showProfileScreen = false },
                securityManager = securityManager,
                onChangePin = { newPin, onResult ->
                    viewModel.updateUserSecurityPin(newPin, securityManager, onResult)
                },
                modifier = modifier
            )
        } else {
            when (NavTab.entries[selectedTabIndex]) {
                NavTab.HOME -> {
                    HomeScreen(
                        uiState = uiState,
                        onAddTransactionClicked = { showAddDialog = true },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onCurrencySelected = { viewModel.setCurrency(it) },
                        onCurrencyIndexSelected = { viewModel.setActiveCurrencyIndex(it) },
                        onProfileClick = { showProfileScreen = true },
                        onGoToScheduledPayments = { selectedTabIndex = 2 },
                        onResetToZero = { viewModel.clearAllDataToZero() },
                        modifier = modifier
                    )
                }
                NavTab.ANALYTICS -> {
                    AnalyticsScreen(
                        uiState = uiState,
                        modifier = modifier
                    )
                }
                NavTab.BUDGET -> {
                    BudgetScreen(
                        uiState = uiState,
                        onSaveBudgetLimit = { cat, limit, thresh ->
                            viewModel.saveBudgetLimit(cat, limit, thresh)
                        },
                        onSetBudgetPeriodMode = { mode ->
                            viewModel.setBudgetPeriodMode(mode)
                        },
                        onAddScheduledExpense = { title, amount, cat, dueDate, notify, attachmentUri, note ->
                            viewModel.addScheduledExpense(title, amount, cat, dueDate, notify, attachmentUri, note)
                        },
                        onScanReceiptWithDueDate = { uri, onResult ->
                            viewModel.scanReceiptWithDueDate(uri, onResult)
                        },
                        onMarkScheduledExpensePaid = { expense ->
                            viewModel.markScheduledExpenseAsPaid(expense)
                        },
                        onDeleteScheduledExpense = { expense ->
                            viewModel.deleteScheduledExpense(expense)
                        },
                        onArchivePeriod = { title, note, clearPeriodData ->
                            viewModel.archiveCurrentPeriod(title, note, clearPeriodData)
                        },
                        onDeleteArchivedPeriod = { period ->
                            viewModel.deleteArchivedPeriod(period)
                        },
                        onCloseArchivedPeriod = { periodId ->
                            viewModel.closeArchivedPeriod(periodId)
                        },
                        onAddTransactionToArchivedPeriod = { periodId, title, amount, category, type, note ->
                            viewModel.addTransactionToArchivedPeriod(periodId, title, amount, category, type, note)
                        },
                        onEditTransactionInArchivedPeriod = { periodId, txIndex, title, amount, category, type, note ->
                            viewModel.editTransactionInArchivedPeriod(periodId, txIndex, title, amount, category, type, note)
                        },
                        onDeleteTransactionFromArchivedPeriod = { periodId, txIndex ->
                            viewModel.deleteTransactionFromArchivedPeriod(periodId, txIndex)
                        },
                        onAddExtraordinaryFund = { title, amount, note ->
                            viewModel.addExtraordinaryFund(title, amount, note)
                        },
                        onUpdateExtraordinaryFund = { fund ->
                            viewModel.updateExtraordinaryFund(fund)
                        },
                        onDeleteExtraordinaryFund = { fund ->
                            viewModel.deleteExtraordinaryFund(fund)
                        },
                        onAddAllocationToExtraordinaryFund = { fundId, title, amount, category, note ->
                            viewModel.addAllocationToExtraordinaryFund(fundId, title, amount, category, note)
                        },
                        onEditAllocationInExtraordinaryFund = { fundId, allocId, title, amount, category, note ->
                            viewModel.editAllocationInExtraordinaryFund(fundId, allocId, title, amount, category, note)
                        },
                        onDeleteAllocationFromExtraordinaryFund = { fundId, allocId ->
                            viewModel.deleteAllocationFromExtraordinaryFund(fundId, allocId)
                        },
                        onTestNotification = {
                            viewModel.testScheduledExpenseReminder()
                        },
                        modifier = modifier
                    )
                }
                NavTab.BACKUP -> {
                    BackupScreen(
                        uiState = uiState,
                        onSetCurrency = { viewModel.setCurrency(it) },
                        onSetThreeCurrencies = { c1, c2, c3, r2, r3 -> viewModel.setThreeCurrencies(c1, c2, c3, r2, r3) },
                        onSyncGoogleDrive = { viewModel.syncToGoogleDrive() },
                        onRestoreGoogleDrive = { viewModel.restoreFromGoogleDrive() },
                        onGenerateExcelCsv = { viewModel.generateExcelCsvReport() },
                        onGenerateBackup = { viewModel.generateBackupJson() },
                        onRestoreBackup = { viewModel.restoreBackupJson(it) },
                        onClearImportMessage = { viewModel.clearImportMessage() },
                        onLogout = { viewModel.logoutUser() },
                        onLogin = { email, name -> viewModel.loginUser(email, name) },
                        onSyncFirebase = { viewModel.syncToFirebase() },
                        modifier = modifier
                    )
                }
            }
        }


        // Add Transaction Dialog Modal
        if (showAddDialog) {
            AddTransactionDialog(
                currencySymbol = uiState.currencySymbol,
                activeCurrencyIndex = uiState.activeCurrencyIndex,
                exchangeRate2 = uiState.exchangeRate2,
                exchangeRate3 = uiState.exchangeRate3,
                isAiLoading = isAiLoading,
                aiErrorMessage = aiErrorMessage,
                onDismiss = { showAddDialog = false },
                onAddManual = { title, amount, category, type, note, timestamp, attachmentUri, dueDate, scheduleReminder ->
                    viewModel.addTransaction(
                        title = title,
                        amount = amount,
                        category = category,
                        type = type,
                        note = note,
                        isAi = false,
                        timestamp = timestamp,
                        attachmentUri = attachmentUri,
                        dueDate = dueDate,
                        schedulePaymentReminder = scheduleReminder
                    )
                },
                onCategorizeRequested = { prompt, onResult ->
                    viewModel.processAiInput(prompt, onResult)
                },
                onConfirmAddTransaction = { res ->
                    val finalAmount = if (res.currency == "USD") {
                        res.amount * uiState.exchangeRate2
                    } else {
                        res.amount
                    }
                    val finalNote = if (res.currency == "USD") {
                        "[${res.amount} USD] ${res.note}"
                    } else {
                        res.note
                    }
                    viewModel.addTransaction(
                        title = res.title,
                        amount = finalAmount,
                        category = res.category,
                        type = res.type,
                        note = finalNote,
                        isAi = true
                    )
                },
                onClearAiError = {
                    viewModel.clearAiError()
                },
                onScanReceiptWithDueDate = { uri, onResult ->
                    viewModel.scanReceiptWithDueDate(uri, onResult)
                }
            )
        }

        // Automatic In-App Update Prompt Dialog
        val availableUpdate = uiState.availableUpdate
        if (availableUpdate != null && availableUpdate.hasUpdate) {
            val context = LocalContext.current
            val activity = context as? Activity
            val coroutineScope = rememberCoroutineScope()
            val updateManager = remember { AppUpdateManager(context) }
            var isDownloadingUpdate by remember { mutableStateOf(false) }
            var downloadProgress by remember { mutableFloatStateOf(0f) }
            var updateError by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = {
                    if (!isDownloadingUpdate) {
                        viewModel.dismissUpdatePrompt()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Actualización",
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Actualización Detectada (v${availableUpdate.latestVersion})",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Se ha detectado una nueva versión del sistema automáticamente. La actualización se descargará e instalará directamente en tu dispositivo sin necesidad de buscar o gestionar archivos externos.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (availableUpdate.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Novedades:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = availableUpdate.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isDownloadingUpdate) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Descargando e instalando... ${(downloadProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryEmerald
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = PrimaryEmerald
                            )
                        }
                        updateError?.let { err ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "⚠️ $err",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (activity != null) {
                                isDownloadingUpdate = true
                                downloadProgress = 0f
                                updateError = null
                                coroutineScope.launch {
                                    val res = updateManager.downloadAndInstallApk(
                                        activity = activity,
                                        apkUrl = availableUpdate.apkDownloadUrl,
                                        onProgress = { downloadProgress = it }
                                    )
                                    isDownloadingUpdate = false
                                    if (res.isFailure) {
                                        updateError = res.exceptionOrNull()?.message
                                    } else {
                                        viewModel.dismissUpdatePrompt()
                                    }
                                }
                            }
                        },
                        enabled = !isDownloadingUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                    ) {
                        Text(if (isDownloadingUpdate) "Actualizando..." else "Actualizar Ahora")
                    }
                },
                dismissButton = {
                    if (!isDownloadingUpdate) {
                        TextButton(onClick = { viewModel.dismissUpdatePrompt() }) {
                            Text("Más tarde")
                        }
                    }
                }
            )
        }
    }
}
