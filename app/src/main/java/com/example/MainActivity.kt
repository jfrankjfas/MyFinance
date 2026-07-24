package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FinanceViewModel
import com.example.ui.components.AddTransactionDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.FinanzasClaraTheme
import com.example.ui.theme.PrimaryEmerald

import androidx.compose.foundation.isSystemInDarkTheme

enum class NavTab(val title: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Default.Home),
    ANALYTICS("Análisis", Icons.Default.PieChart),
    BUDGET("Presupuesto", Icons.Default.AccountBalance),
    BACKUP("Respaldo", Icons.Default.Cloud)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FinanceViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = when (uiState.appTheme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            FinanzasClaraTheme(darkTheme = isDark) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: FinanceViewModel = viewModel()) {
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
                onLogin = { email, name -> viewModel.loginUser(email, name) },
                onSetTheme = { viewModel.setAppTheme(it) },
                onSetLanguage = { viewModel.setAppLanguage(it) },
                onBack = { showProfileScreen = false },
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
                        onAddScheduledExpense = { title, amount, cat, dueDate, notify ->
                            viewModel.addScheduledExpense(title, amount, cat, dueDate, notify)
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
                        modifier = modifier
                    )
                }
            }
        }


        // Add Transaction Dialog Modal
        if (showAddDialog) {
            AddTransactionDialog(
                currencySymbol = uiState.currencySymbol,
                isAiLoading = isAiLoading,
                aiErrorMessage = aiErrorMessage,
                onDismiss = { showAddDialog = false },
                onAddManual = { title, amount, category, type, note ->
                    viewModel.addTransaction(title, amount, category, type, note, isAi = false)
                },
                onCategorizeRequested = { prompt, onResult ->
                    viewModel.processAiInput(prompt, onResult)
                },
                onConfirmAddTransaction = { res ->
                    viewModel.addTransaction(
                        title = res.title,
                        amount = res.amount,
                        category = res.category,
                        type = res.type,
                        note = res.note,
                        isAi = true
                    )
                },
                onClearAiError = {
                    viewModel.clearAiError()
                }
            )
        }
    }
}
