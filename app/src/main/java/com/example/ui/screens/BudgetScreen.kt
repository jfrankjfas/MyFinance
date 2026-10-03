package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import com.example.ai.ReceiptScanResult
import com.example.data.AttachmentStorageHelper
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Archive
import com.example.data.entity.ArchivedPeriodEntity
import com.example.data.entity.ExtraordinaryFundEntity
import com.example.data.entity.ScheduledExpenseEntity
import com.example.ui.FinanceUiState
import com.example.ui.components.ArchiveConfirmDialog
import com.example.ui.components.ArchivedStatementsListDialog
import com.example.ui.components.BankStatementDetailDialog
import com.example.ui.components.BudgetProgressBarCard
import com.example.ui.components.ExtraordinaryFundsSection
import com.example.ui.components.categoryList
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    uiState: FinanceUiState,
    onSaveBudgetLimit: (category: String, limit: Double, threshold: Int) -> Unit,
    onDeleteBudget: (category: String) -> Unit = {},
    onSetBudgetPeriodMode: (mode: String) -> Unit,
    onAddScheduledExpense: (title: String, amount: Double, category: String, dueDate: Long, notify: Boolean, attachmentUri: String?, note: String, isRecurring: Boolean, fundingSource: String, extraordinaryFundId: Long?, extraordinaryFundTitle: String) -> Unit,
    onUpdateScheduledExpense: (id: Long, title: String, amount: Double, category: String, dueDate: Long, notify: Boolean, attachmentUri: String?, note: String, isPriority: Boolean, isRecurring: Boolean, fundingSource: String, extraordinaryFundId: Long?, extraordinaryFundTitle: String) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onMarkScheduledExpensePaid: (expense: ScheduledExpenseEntity, chosenFundingSource: String?, chosenFundId: Long?, chosenFundTitle: String?) -> Unit,
    onDeleteScheduledExpense: (expense: ScheduledExpenseEntity) -> Unit,
    onScanReceiptWithDueDate: ((Uri, (ReceiptScanResult) -> Unit) -> Unit)? = null,
    onArchivePeriod: (title: String, note: String, clearPeriodData: Boolean) -> Unit = { _, _, _ -> },
    onArchiveAndClosePeriod: (title: String, note: String, periodMode: String, startTimestamp: Long?, endTimestamp: Long?, clearPeriodData: Boolean, closePermanently: Boolean) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteArchivedPeriod: (ArchivedPeriodEntity) -> Unit = {},
    onCloseArchivedPeriod: (Long) -> Unit = {},
    onAddTransactionToArchivedPeriod: (periodId: Long, title: String, amount: Double, category: String, type: String, note: String) -> Unit = { _, _, _, _, _, _ -> },
    onEditTransactionInArchivedPeriod: (periodId: Long, txIndex: Int, title: String, amount: Double, category: String, type: String, note: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteTransactionFromArchivedPeriod: (periodId: Long, txIndex: Int) -> Unit = { _, _ -> },
    onAddExtraordinaryFund: (title: String, totalAmount: Double, note: String) -> Unit = { _, _, _ -> },
    onUpdateExtraordinaryFund: (ExtraordinaryFundEntity) -> Unit = {},
    onDeleteExtraordinaryFund: (ExtraordinaryFundEntity) -> Unit = {},
    onAddAllocationToExtraordinaryFund: (fundId: Long, title: String, amount: Double, category: String, note: String) -> Unit = { _, _, _, _, _ -> },
    onEditAllocationInExtraordinaryFund: (fundId: Long, allocationId: String, title: String, amount: Double, category: String, note: String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteAllocationFromExtraordinaryFund: (fundId: Long, allocationId: String) -> Unit = { _, _ -> },
    onTestNotification: () -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddScheduledDialog by remember { mutableStateOf(false) }
    var editingScheduledExpense by remember { mutableStateOf<ScheduledExpenseEntity?>(null) }
    var payingScheduledExpense by remember { mutableStateOf<ScheduledExpenseEntity?>(null) }
    var showArchiveConfirmDialog by remember { mutableStateOf(false) }
    var showArchivedStatementsList by remember { mutableStateOf(false) }
    var selectedArchivedStatement by remember { mutableStateOf<ArchivedPeriodEntity?>(null) }

    val context = LocalContext.current
    val activeSymbol = uiState.currencySymbol
    var generalLimitInput by remember(uiState.budgets, activeSymbol, uiState.activeConversionMultiplier) {
        val general = uiState.budgets.find { it.category == "GENERAL" }
        val rawLimit = general?.limitAmount ?: 0.0
        val displayLimit = rawLimit * uiState.activeConversionMultiplier
        mutableStateOf(if (displayLimit > 0) String.format(Locale.US, "%.2f", displayLimit) else "")
    }

    var showCategoryBudgetDialog by remember { mutableStateOf(false) }
    var editingBudgetCategory by remember { mutableStateOf<String?>(null) }
    var selectedCatForBudget by remember { mutableStateOf("Alimentación") }
    var catLimitInput by remember { mutableStateOf("") }

    val isRefreshing = uiState.isAiLoading || uiState.firebaseSyncStatus.isSyncing

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Screen Header
        item {
            Column {
                Text(
                    text = "Presupuesto y Gastos Programados",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gestiona tu periodo (Mensual/Quincenal) y programa pagos con notificaciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Period Selector Chips
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📅 Definir Periodo de Trabajo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.budgetPeriodMode == "MONTHLY",
                            onClick = { onSetBudgetPeriodMode("MONTHLY") },
                            label = { Text("Mensual") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = uiState.budgetPeriodMode == "FORTNIGHT_1",
                            onClick = { onSetBudgetPeriodMode("FORTNIGHT_1") },
                            label = { Text("1ª Quincena") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = uiState.budgetPeriodMode == "FORTNIGHT_2",
                            onClick = { onSetBudgetPeriodMode("FORTNIGHT_2") },
                            label = { Text("2ª Quincena") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Action Bar: Archiving & Consultation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Finalizar y Consultar Estados de Cuenta",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showArchiveConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Respaldar Periodo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showArchivedStatementsList = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver Histórico (${uiState.archivedPeriods.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Period Financial Summary & Budget Analysis Card
        item {
            val periodLabel = when (uiState.budgetPeriodMode) {
                "FORTNIGHT_1" -> "1ª Quincena (Días 1-15)"
                "FORTNIGHT_2" -> "2ª Quincena (Días 16-Fin)"
                else -> "Mes Completo"
            }

            val totalCommitted = uiState.periodSpentTotal + uiState.periodPendingExpensesTotal
            val rawRemaining = uiState.periodBudgetLimitTotal - totalCommitted
            val executionPercent = if (uiState.periodBudgetLimitTotal > 0) {
                ((totalCommitted / uiState.periodBudgetLimitTotal) * 100).toInt()
            } else 0

            val cal = Calendar.getInstance()
            val curDay = cal.get(Calendar.DAY_OF_MONTH)
            val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val (pStart, pEnd) = when (uiState.budgetPeriodMode) {
                "FORTNIGHT_1" -> Pair(1, 15)
                "FORTNIGHT_2" -> Pair(16, maxDaysInMonth)
                else -> Pair(1, maxDaysInMonth)
            }
            val totalDays = (pEnd - pStart + 1).coerceAtLeast(1)
            val elapsedDays = (curDay - pStart + 1).coerceIn(1, totalDays)
            val remainingDays = (pEnd - curDay).coerceAtLeast(0)
            val dailyBurn = if (elapsedDays > 0) uiState.periodSpentTotal / elapsedDays else 0.0
            val dailyTarget = if (remainingDays > 0 && rawRemaining > 0) rawRemaining / remainingDays else 0.0

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📊 Análisis de Presupuesto",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ejecución $periodLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val badgeColor = when {
                            executionPercent >= 100 -> ExpenseRed
                            executionPercent >= 80 -> WarningAmber
                            else -> IncomeGreen
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (executionPercent >= 100) "$executionPercent% Excedido" else "$executionPercent% Consumido",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = (executionPercent / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = when {
                            executionPercent >= 100 -> ExpenseRed
                            executionPercent >= 80 -> WarningAmber
                            else -> PrimaryEmerald
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Asignado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.2f", uiState.periodBudgetLimitTotal)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text("Gastado Real", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.2f", uiState.periodSpentTotal)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }

                        Column {
                            Text("Comprometido", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.2f", uiState.periodPendingExpensesTotal)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                        }

                        Column {
                            Text(if (rawRemaining >= 0) "Disponible" else "Déficit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${if (rawRemaining >= 0) "+" else "-"}${uiState.currencySymbol}${String.format("%.2f", kotlin.math.abs(rawRemaining))}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (rawRemaining >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Daily pacing pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Día $elapsedDays/$totalDays • Gasto promedio: ${uiState.currencySymbol}${String.format("%.2f", dailyBurn)}/día • Margen sugerido: ${uiState.currencySymbol}${String.format("%.2f", dailyTarget)}/día",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons to Save EC / View Historical EC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showArchiveConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar EC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showArchivedStatementsList = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver EC (${uiState.archivedPeriods.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Scheduled Payments / Expenses
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gastos Programados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Notificaciones automáticas 2 días y 1 día antes del pago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showAddScheduledDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Programar", maxLines = 1, fontSize = 12.sp)
                }
            }
        }

        if (uiState.scheduledExpenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sin gastos programados registrados",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Toca '+ Programar' para registrar facturas, renta o servicios para cualquier fecha futura.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(uiState.scheduledExpenses) { item ->
                ScheduledExpenseItemCard(
                    item = item,
                    currencySymbol = uiState.currencySymbol,
                    onEdit = { editingScheduledExpense = item },
                    onMarkPaid = { payingScheduledExpense = item },
                    onDelete = { onDeleteScheduledExpense(item) }
                )
            }
        }

        // Configure General Budget Limit Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Configurar Presupuesto Mensual",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Para Quincenas se divide automáticamente en 2 partes iguales.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onTestNotification) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Probar Notificación", tint = WarningAmber)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = generalLimitInput,
                        onValueChange = { generalLimitInput = it },
                        label = { Text("Límite Mensual General (${uiState.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                editingBudgetCategory = null
                                selectedCatForBudget = categoryList.firstOrNull { it != "Varios" && it != "Otro" } ?: "Alimentación"
                                catLimitInput = ""
                                showCategoryBudgetDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Categoría", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val parsed = generalLimitInput.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
                                if (parsed > 0) {
                                    onSaveBudgetLimit("GENERAL", parsed, 80)
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Guardar Límite")
                        }
                    }
                }
            }
        }

        // Section Title: Progress List
        item {
            Text(
                text = "Progreso de Presupuesto del Periodo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.budgetProgresses.isEmpty()) {
            item {
                Text(
                    text = "Aún no has configurado presupuestos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.budgetProgresses) { progress ->
                val isCustomCat = !progress.category.startsWith("Presupuesto")
                BudgetProgressBarCard(
                    progress = progress,
                    currencySymbol = uiState.currencySymbol,
                    onEdit = if (isCustomCat) {
                        {
                            editingBudgetCategory = progress.category
                            selectedCatForBudget = progress.category
                            val existingBudget = uiState.budgets.find { it.category == progress.category }
                            val rawAmt = existingBudget?.limitAmount ?: (progress.limitAmount / uiState.activeConversionMultiplier)
                            catLimitInput = String.format(Locale.US, "%.2f", rawAmt * uiState.activeConversionMultiplier)
                            showCategoryBudgetDialog = true
                        }
                    } else null,
                    onDelete = if (isCustomCat) { { onDeleteBudget(progress.category) } } else null
                )
            }
        }

        // Section: Extraordinary Funds
        item {
            ExtraordinaryFundsSection(
                funds = uiState.extraordinaryFunds,
                currencySymbol = uiState.currencySymbol,
                onAddFund = onAddExtraordinaryFund,
                onUpdateFund = onUpdateExtraordinaryFund,
                onDeleteFund = onDeleteExtraordinaryFund,
                onAddAllocation = onAddAllocationToExtraordinaryFund,
                onEditAllocation = onEditAllocationInExtraordinaryFund,
                onDeleteAllocation = onDeleteAllocationFromExtraordinaryFund
            )
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
    }

    // Add Scheduled Expense Dialog
    if (showAddScheduledDialog) {
        AddScheduledExpenseDialog(
            currencySymbol = uiState.currencySymbol,
            extraordinaryFunds = uiState.extraordinaryFunds,
            initialExpense = null,
            onDismiss = { showAddScheduledDialog = false },
            onScanReceiptWithDueDate = onScanReceiptWithDueDate,
            onConfirm = { title, amount, category, dueDate, notify, attachmentUri, note, isRecurring, fundingSource, fundId, fundTitle ->
                onAddScheduledExpense(title, amount, category, dueDate, notify, attachmentUri, note, isRecurring, fundingSource, fundId, fundTitle)
                showAddScheduledDialog = false
            }
        )
    }

    // Edit Scheduled Expense Dialog
    editingScheduledExpense?.let { expenseToEdit ->
        AddScheduledExpenseDialog(
            currencySymbol = uiState.currencySymbol,
            extraordinaryFunds = uiState.extraordinaryFunds,
            initialExpense = expenseToEdit,
            onDismiss = { editingScheduledExpense = null },
            onScanReceiptWithDueDate = onScanReceiptWithDueDate,
            onConfirm = { title, amount, category, dueDate, notify, attachmentUri, note, isRecurring, fundingSource, fundId, fundTitle ->
                onUpdateScheduledExpense(
                    expenseToEdit.id,
                    title,
                    amount,
                    category,
                    dueDate,
                    notify,
                    attachmentUri,
                    note,
                    expenseToEdit.isEmergencyPriority,
                    isRecurring,
                    fundingSource,
                    fundId,
                    fundTitle
                )
                editingScheduledExpense = null
            }
        )
    }

    // Pay Scheduled Expense Dialog (Choose where to take the money from)
    payingScheduledExpense?.let { expenseToPay ->
        PayScheduledExpenseSourceDialog(
            expense = expenseToPay,
            extraordinaryFunds = uiState.extraordinaryFunds,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { payingScheduledExpense = null },
            onConfirmPay = { chosenSource, chosenFundId, chosenFundTitle ->
                onMarkScheduledExpensePaid(expenseToPay, chosenSource, chosenFundId, chosenFundTitle)
                payingScheduledExpense = null
            }
        )
    }

    // Category Budget Dialog
    if (showCategoryBudgetDialog) {
        var catExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = {
                showCategoryBudgetDialog = false
                editingBudgetCategory = null
            },
            title = {
                Text(
                    text = if (editingBudgetCategory != null) "Editar Presupuesto de $selectedCatForBudget" else "Presupuesto por Categoría",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Asigna un límite mensual a una categoría específica (ej. Inversión, Alimentación, Servicios, etc.):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExposedDropdownMenuBox(
                        expanded = catExpanded,
                        onExpandedChange = { catExpanded = !catExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCatForBudget,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            categoryList.filter { it != "Varios" }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCatForBudget = cat
                                        catExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = catLimitInput,
                        onValueChange = { catLimitInput = it },
                        label = { Text("Límite Mensual (${uiState.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = catLimitInput.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
                        if (parsed > 0) {
                            if (editingBudgetCategory != null && editingBudgetCategory != selectedCatForBudget) {
                                onDeleteBudget(editingBudgetCategory!!)
                            }
                            onSaveBudgetLimit(selectedCatForBudget, parsed, 80)
                            showCategoryBudgetDialog = false
                            editingBudgetCategory = null
                            catLimitInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingBudgetCategory != null) {
                        TextButton(
                            onClick = {
                                onDeleteBudget(editingBudgetCategory!!)
                                showCategoryBudgetDialog = false
                                editingBudgetCategory = null
                                catLimitInput = ""
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar", color = ExpenseRed)
                        }
                    }
                    TextButton(onClick = {
                        showCategoryBudgetDialog = false
                        editingBudgetCategory = null
                        catLimitInput = ""
                    }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    // Archiving & Statement Dialogs (2-Step Pre-Cierre & Official Lock)
    if (showArchiveConfirmDialog) {
        ArchiveConfirmDialog(
            periodMode = uiState.budgetPeriodMode,
            budgetLimit = uiState.periodBudgetLimitTotal,
            spentTotal = uiState.periodSpentTotal,
            scheduledTotal = uiState.periodPendingExpensesTotal,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showArchiveConfirmDialog = false },
            onConfirm = { title, note, mode, sMs, eMs, clearPeriodData, closePermanently ->
                showArchiveConfirmDialog = false
                onArchiveAndClosePeriod(title, note, mode, sMs, eMs, clearPeriodData, closePermanently)
            }
        )
    }

    if (showArchivedStatementsList) {
        ArchivedStatementsListDialog(
            archivedPeriods = uiState.archivedPeriods,
            onDismiss = { showArchivedStatementsList = false },
            onSelectStatement = { statement ->
                selectedArchivedStatement = statement
            },
            onDeleteStatement = { statement ->
                onDeleteArchivedPeriod(statement)
            }
        )
    }

    selectedArchivedStatement?.let { statement ->
        // Retrieve fresh updated statement from uiState if modified
        val currentStatement = uiState.archivedPeriods.find { it.id == statement.id } ?: statement
        BankStatementDetailDialog(
            item = currentStatement,
            onDismiss = { selectedArchivedStatement = null },
            onCloseStatement = { id ->
                onCloseArchivedPeriod(id)
            },
            onAddTransaction = { id, title, amount, category, type, note ->
                onAddTransactionToArchivedPeriod(id, title, amount, category, type, note)
            },
            onEditTransaction = { id, txIndex, title, amount, category, type, note ->
                onEditTransactionInArchivedPeriod(id, txIndex, title, amount, category, type, note)
            },
            onDeleteTransaction = { id, txIndex ->
                onDeleteTransactionFromArchivedPeriod(id, txIndex)
            }
        )
    }
}

@Composable
fun ScheduledExpenseItemCard(
    item: ScheduledExpenseEntity,
    currencySymbol: String,
    onEdit: (() -> Unit)? = null,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("dd 'de' MMM, yyyy", Locale("es", "ES"))
    val formattedDate = sdf.format(Date(item.dueDate))
    val categoryIcon = getCategoryIcon(item.category)

    val now = System.currentTimeMillis()
    val dayMs = 86400000L
    val diffMs = item.dueDate - now
    val daysRemaining = (diffMs / dayMs).toInt()

    var showImageModal by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (!item.isPaid && daysRemaining in 0..2) ExpenseRed.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (item.isPaid) IncomeGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = if (item.isPaid) IncomeGreen else WarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.category} • Vence: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.padding(top = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sourceBadge = when (item.fundingSource) {
                            "EXTRAORDINARY_FUND" -> "🌟 Fondo: ${item.extraordinaryFundTitle.ifBlank { "Extraordinario" }}"
                            "SAVINGS" -> "🏦 Ahorros / Otros"
                            else -> "💰 Presupuesto del Período"
                        }
                        val sourceColor = when (item.fundingSource) {
                            "EXTRAORDINARY_FUND" -> PrimaryEmerald
                            "SAVINGS" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = sourceColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = sourceBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = sourceColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        if (item.isRecurringMonthly) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SleekPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "🔁 Recurrente",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "$currencySymbol${String.format("%.2f", item.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isPaid) MaterialTheme.colorScheme.onSurface else ExpenseRed
                )
            }

            // Attachment and Urgency Badge
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Countdown Badge
                if (item.isPaid) {
                    Surface(
                        color = IncomeGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "✔ PAGADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    val (badgeText, badgeColor) = when {
                        daysRemaining == 2 -> "⏳ VENCE EN 2 DÍAS (Alerta activa)" to WarningAmber
                        daysRemaining == 1 -> "🚨 ¡VENCE MAÑANA! (Alerta activa)" to ExpenseRed
                        daysRemaining == 0 -> "📅 ¡VENCE HOY!" to ExpenseRed
                        daysRemaining < 0 -> "⚠️ VENCIDO HACE ${-daysRemaining} DÍAS" to ExpenseRed
                        else -> "🗓️ VENCE EN $daysRemaining DÍAS" to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Attachment preview thumbnail if any
                if (!item.attachmentUri.isNullOrBlank()) {
                    Surface(
                        color = PrimaryEmerald.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { showImageModal = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ver Recibo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = if (item.notifyReminder) "🔔 Alerta 2 días y 1 día antes" else "🔕 Sin recordatorio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.isRecurringMonthly) {
                        Text(
                            text = "🔁 Recurrente mensual automático",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!item.isPaid) {
                        TextButton(onClick = onMarkPaid) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = IncomeGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Marcar Pagado", fontSize = 12.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                        }
                    }

                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar gasto programado?", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar '${item.title}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showImageModal && !item.attachmentUri.isNullOrBlank()) {
        Dialog(onDismissRequest = { showImageModal = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comprobante: ${item.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showImageModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = item.attachmentUri,
                            contentDescription = "Comprobante",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduledExpenseDialog(
    currencySymbol: String,
    extraordinaryFunds: List<ExtraordinaryFundEntity> = emptyList(),
    initialExpense: ScheduledExpenseEntity? = null,
    onDismiss: () -> Unit,
    onScanReceiptWithDueDate: ((Uri, (ReceiptScanResult) -> Unit) -> Unit)? = null,
    onConfirm: (title: String, amount: Double, category: String, dueDate: Long, notify: Boolean, attachmentUri: String?, note: String, isRecurring: Boolean, fundingSource: String, extraordinaryFundId: Long?, extraordinaryFundTitle: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember(initialExpense) { mutableStateOf(initialExpense?.title ?: "") }
    var amountText by remember(initialExpense) {
        mutableStateOf(
            if (initialExpense != null && initialExpense.amount > 0)
                String.format(Locale.US, "%.2f", initialExpense.amount)
            else ""
        )
    }
    var selectedCategory by remember(initialExpense) { mutableStateOf(initialExpense?.category ?: "Servicios") }
    var notifyReminder by remember(initialExpense) { mutableStateOf(initialExpense?.notifyReminder ?: true) }
    var isRecurringMonthly by remember(initialExpense) { mutableStateOf(initialExpense?.isRecurringMonthly ?: false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var fundingSource by remember(initialExpense) { mutableStateOf(initialExpense?.fundingSource ?: "PERIOD_BUDGET") }
    var extraordinaryFundId by remember(initialExpense) { mutableStateOf(initialExpense?.extraordinaryFundId) }
    var extraordinaryFundTitle by remember(initialExpense) { mutableStateOf(initialExpense?.extraordinaryFundTitle ?: "") }
    var fundDropdownExpanded by remember { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }
    var selectedDueDateMs by remember(initialExpense) {
        mutableLongStateOf(initialExpense?.dueDate ?: (calendar.timeInMillis + (3 * 86400000L)))
    }

    val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
    val formattedSelectedDate = sdf.format(Date(selectedDueDateMs))

    var attachedImageUri by remember(initialExpense) { mutableStateOf<String?>(initialExpense?.attachmentUri) }
    var isScanningAttachment by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val persistentUri = AttachmentStorageHelper.copyUriToInternalStorage(context, uri)
            attachedImageUri = persistentUri ?: uri.toString()

            val parsedUri = Uri.parse(attachedImageUri)
            isScanningAttachment = true
            onScanReceiptWithDueDate?.invoke(parsedUri) { res ->
                isScanningAttachment = false
                if (title.isBlank() && res.title.isNotBlank()) title = res.title
                if (amountText.isBlank() && res.amount > 0) amountText = String.format(Locale.US, "%.2f", res.amount)
                if (res.category.isNotBlank() && categoryList.contains(res.category)) selectedCategory = res.category
                if (res.dueDateMs != null) {
                    selectedDueDateMs = res.dueDateMs
                    notifyReminder = true
                }
            } ?: run {
                isScanningAttachment = false
            }
        }
    }

    fun showDatePicker() {
        val c = Calendar.getInstance().apply { timeInMillis = selectedDueDateMs }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    clear()
                    set(year, month, dayOfMonth, 12, 0, 0)
                }
                selectedDueDateMs = newCal.timeInMillis
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Programar Pago / Vencimiento", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Optional Attachment Picker
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (attachedImageUri == null) "Adjuntar Recibo" else "Recibo Adjuntado",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (attachedImageUri == null) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Foto", fontSize = 12.sp)
                            }
                        } else {
                            IconButton(onClick = { attachedImageUri = null }) {
                                Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (isScanningAttachment) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = PrimaryEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detectando fecha de pago en el recibo...", fontSize = 11.sp, color = PrimaryEmerald)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Concepto (Ej. Renta, Factura Luz, Tarjeta)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoryList.forEach { catName ->
                            DropdownMenuItem(
                                text = { Text(catName) },
                                onClick = {
                                    selectedCategory = catName
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Picker Field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formattedSelectedDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha de Pago / Vencimiento") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha", tint = WarningAmber)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker() }
                    )
                }

                // Funding Source Selector
                Text(
                    text = "💰 Origen del dinero para pagar:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = fundingSource == "PERIOD_BUDGET",
                        onClick = { fundingSource = "PERIOD_BUDGET" },
                        label = { Text("Presupuesto", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryEmerald,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = fundingSource == "EXTRAORDINARY_FUND",
                        onClick = {
                            fundingSource = "EXTRAORDINARY_FUND"
                            if (extraordinaryFundId == null && extraordinaryFunds.isNotEmpty()) {
                                extraordinaryFundId = extraordinaryFunds.first().id
                                extraordinaryFundTitle = extraordinaryFunds.first().title
                            }
                        },
                        label = { Text("Fondo Extr.", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryEmerald,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = fundingSource == "SAVINGS",
                        onClick = { fundingSource = "SAVINGS" },
                        label = { Text("Ahorros", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryEmerald,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (fundingSource == "EXTRAORDINARY_FUND") {
                    if (extraordinaryFunds.isEmpty()) {
                        Text(
                            text = "⚠️ No hay fondos extraordinarios registrados. Puedes crearlos en la sección 'Ingresos y Fondos Extraordinarios' más abajo.",
                            fontSize = 11.sp,
                            color = WarningAmber
                        )
                    } else {
                        val currentFund = extraordinaryFunds.find { it.id == extraordinaryFundId } ?: extraordinaryFunds.first()
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { fundDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Fondo seleccionado:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(currentFund.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = fundDropdownExpanded,
                                onDismissRequest = { fundDropdownExpanded = false }
                            ) {
                                extraordinaryFunds.forEach { fund ->
                                    DropdownMenuItem(
                                        text = { Text(fund.title) },
                                        onClick = {
                                            extraordinaryFundId = fund.id
                                            extraordinaryFundTitle = fund.title
                                            fundDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🔔 Recordatorio Automático", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text("Notifica 2 días antes y 1 día antes del pago", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = notifyReminder,
                        onCheckedChange = { notifyReminder = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = WarningAmber)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🔁 Recurrente Mensual", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text("Se renueva para el siguiente mes automáticamente al pagar", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isRecurringMonthly,
                        onCheckedChange = { isRecurringMonthly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryEmerald)
                    )
                }
            }
        },
        confirmButton = {
            val cleanAmount = amountText.replace(',', '.').trim()
            val parsedAmount = cleanAmount.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (title.isNotBlank() && parsedAmount > 0) {
                        onConfirm(title.trim(), parsedAmount, selectedCategory, selectedDueDateMs, notifyReminder, attachedImageUri, "", isRecurringMonthly, fundingSource, extraordinaryFundId, extraordinaryFundTitle)
                    }
                },
                enabled = title.isNotBlank() && parsedAmount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
            ) {
                Text(if (initialExpense != null) "Actualizar" else "Programar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun PayScheduledExpenseSourceDialog(
    expense: ScheduledExpenseEntity,
    extraordinaryFunds: List<ExtraordinaryFundEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmPay: (fundingSource: String, fundId: Long?, fundTitle: String) -> Unit
) {
    var selectedSource by remember { mutableStateOf(expense.fundingSource) }
    var selectedFundId by remember { mutableStateOf(expense.extraordinaryFundId ?: extraordinaryFunds.firstOrNull()?.id) }
    var fundDropdownExpanded by remember { mutableStateOf(false) }

    val selectedFund = extraordinaryFunds.find { it.id == selectedFundId } ?: extraordinaryFunds.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar Pago", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with expense summary
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%.2f", expense.amount)} • Categoría: ${expense.category}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (expense.isRecurringMonthly) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SleekPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "🔁 Pago Recurrente Mensual: Al confirmar el pago, la siguiente cuota se programará automáticamente para el próximo mes.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SleekPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "¿De dónde deseas tomar el dinero para pagar?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Option 1: Presupuesto asignado en el período
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSource = "PERIOD_BUDGET" },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        if (selectedSource == "PERIOD_BUDGET") 2.dp else 1.dp,
                        if (selectedSource == "PERIOD_BUDGET") PrimaryEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedSource == "PERIOD_BUDGET") PrimaryEmerald.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSource == "PERIOD_BUDGET",
                            onClick = { selectedSource = "PERIOD_BUDGET" },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryEmerald)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("💰 Presupuesto Asignado del Período", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Se descuenta del presupuesto del período activo.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Option 2: Fondos Extraordinarios
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSource = "EXTRAORDINARY_FUND" },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        if (selectedSource == "EXTRAORDINARY_FUND") 2.dp else 1.dp,
                        if (selectedSource == "EXTRAORDINARY_FUND") PrimaryEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedSource == "EXTRAORDINARY_FUND") PrimaryEmerald.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedSource == "EXTRAORDINARY_FUND",
                                onClick = { selectedSource = "EXTRAORDINARY_FUND" },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryEmerald)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("🌟 Fondos Extraordinarios", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Se toma de un fondo extraordinario sin afectar el presupuesto del período.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (selectedSource == "EXTRAORDINARY_FUND") {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (extraordinaryFunds.isEmpty()) {
                                Text(
                                    text = "⚠️ No tienes fondos extraordinarios registrados. Puedes crearlos en la sección 'Ingresos y Fondos Extraordinarios' más abajo.",
                                    fontSize = 11.sp,
                                    color = WarningAmber
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().padding(start = 36.dp)) {
                                    OutlinedCard(
                                        onClick = { fundDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = selectedFund?.title ?: "Seleccionar fondo...",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                                if (selectedFund != null) {
                                                    val allocated = try {
                                                        val arr = org.json.JSONArray(selectedFund.allocationsJson)
                                                        var s = 0.0
                                                        for (i in 0 until arr.length()) s += arr.getJSONObject(i).optDouble("amount", 0.0)
                                                        s
                                                    } catch (e: Exception) { 0.0 }
                                                    val available = (selectedFund.totalAmount - allocated).coerceAtLeast(0.0)
                                                    Text(
                                                        text = "Saldo disp: $currencySymbol${String.format(Locale.US, "%.2f", available)}",
                                                        fontSize = 10.sp,
                                                        color = PrimaryEmerald
                                                    )
                                                }
                                            }
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = fundDropdownExpanded,
                                        onDismissRequest = { fundDropdownExpanded = false }
                                    ) {
                                        extraordinaryFunds.forEach { fund ->
                                            val allocated = try {
                                                val arr = org.json.JSONArray(fund.allocationsJson)
                                                var s = 0.0
                                                for (i in 0 until arr.length()) s += arr.getJSONObject(i).optDouble("amount", 0.0)
                                                s
                                            } catch (e: Exception) { 0.0 }
                                            val available = (fund.totalAmount - allocated).coerceAtLeast(0.0)
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(fund.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text("Disponible: $currencySymbol${String.format(Locale.US, "%.2f", available)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                onClick = {
                                                    selectedFundId = fund.id
                                                    fundDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Option 3: Ahorros / Otros Recursos
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSource = "SAVINGS" },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        if (selectedSource == "SAVINGS") 2.dp else 1.dp,
                        if (selectedSource == "SAVINGS") PrimaryEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedSource == "SAVINGS") PrimaryEmerald.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSource == "SAVINGS",
                            onClick = { selectedSource = "SAVINGS" },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryEmerald)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("🏦 Ahorros / Otros Recursos", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Se paga con ahorros propios sin consumir el presupuesto asignado.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fundTitle = if (selectedSource == "EXTRAORDINARY_FUND") (selectedFund?.title ?: "") else ""
                    onConfirmPay(selectedSource, if (selectedSource == "EXTRAORDINARY_FUND") selectedFundId else null, fundTitle)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Confirmar y Pagar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


