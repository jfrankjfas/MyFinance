package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CategoryExpense
import com.example.ui.FinanceUiState
import com.example.ui.components.MonthlyExpenseDonutChartCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.WarningAmber
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    uiState: FinanceUiState,
    onSetBudgetPeriodMode: ((String) -> Unit)? = null,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedPeriodMode by remember(uiState.budgetPeriodMode) { mutableStateOf(uiState.budgetPeriodMode) }

    fun isTimestampInSelectedPeriod(timestamp: Long, mode: String): Boolean {
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
            else -> true
        }
    }

    // Filter transactions by selected period
    val periodTransactions = remember(uiState.transactions, selectedPeriodMode) {
        uiState.transactions.filter { isTimestampInSelectedPeriod(it.timestamp, selectedPeriodMode) }
    }

    var periodIncome = 0.0
    var periodExpense = 0.0
    val periodCategoryMap = mutableMapOf<String, Double>()

    periodTransactions.forEach { tx ->
        if (tx.type == "INCOME") {
            periodIncome += tx.amount
        } else {
            periodExpense += tx.amount
            periodCategoryMap[tx.category] = (periodCategoryMap[tx.category] ?: 0.0) + tx.amount
        }
    }

    val periodCategoryExpenses = remember(periodCategoryMap, periodExpense) {
        if (periodExpense > 0) {
            periodCategoryMap.map { (cat, amt) ->
                CategoryExpense(
                    category = cat,
                    totalAmount = amt,
                    percentage = ((amt / periodExpense) * 100).toFloat()
                )
            }.sortedByDescending { it.totalAmount }
        } else {
            emptyList()
        }
    }

    val periodBalance = periodIncome - periodExpense
    val generalBudget = uiState.budgets.find { it.category == "GENERAL" }
    val generalLimitBase = generalBudget?.limitAmount ?: 0.0
    val periodGeneralLimit = if (generalLimitBase > 0) {
        val baseLimit = if (selectedPeriodMode == "MONTHLY") generalLimitBase else (generalLimitBase / 2.0)
        baseLimit * (if (uiState.currencySymbol == "$" && uiState.exchangeRate2 > 0) (1.0 / uiState.exchangeRate2) else 1.0)
    } else {
        0.0
    }

    val periodPendingScheduled = remember(uiState.scheduledExpenses, selectedPeriodMode) {
        uiState.scheduledExpenses.filter { isTimestampInSelectedPeriod(it.dueDate, selectedPeriodMode) && !it.isPaid }
    }
    val periodPendingTotal = remember(periodPendingScheduled) {
        periodPendingScheduled.sumOf { it.amount }
    }
    val totalCommitted = periodExpense + periodPendingTotal
    val budgetRemaining = (periodGeneralLimit - totalCommitted)

    val periodLabel = when (selectedPeriodMode) {
        "FORTNIGHT_1" -> "1ª Quincena (1-15)"
        "FORTNIGHT_2" -> "2ª Quincena (16-Fin)"
        else -> "Mes Completo"
    }

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

        // Title
        item {
            Column {
                Text(
                    text = "Análisis Financiero",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Control quincenal y mensual de ingresos, gastos y presupuesto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Period Selection Chips (Quincena 1, Quincena 2, Mes)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📅 Periodo de Análisis",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedPeriodMode == "MONTHLY",
                            onClick = {
                                selectedPeriodMode = "MONTHLY"
                                onSetBudgetPeriodMode?.invoke("MONTHLY")
                            },
                            label = { Text("Mes") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedPeriodMode == "FORTNIGHT_1",
                            onClick = {
                                selectedPeriodMode = "FORTNIGHT_1"
                                onSetBudgetPeriodMode?.invoke("FORTNIGHT_1")
                            },
                            label = { Text("1ª Quincena") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedPeriodMode == "FORTNIGHT_2",
                            onClick = {
                                selectedPeriodMode = "FORTNIGHT_2"
                                onSetBudgetPeriodMode?.invoke("FORTNIGHT_2")
                            },
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

        // Period Summary Card: Income vs Expense vs Balance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Balance del Periodo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = periodLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ingresos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.2f", periodIncome)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gastos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.2f", periodExpense)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }

                        Column {
                            Text("Neto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${if (periodBalance >= 0) "+" else ""}${uiState.currencySymbol}${String.format("%.2f", periodBalance)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (periodBalance >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }
        }

        // Budget vs Real Analysis Card (Presupuesto vs Real)
        item {
            val executionPct = if (periodGeneralLimit > 0) {
                ((totalCommitted / periodGeneralLimit) * 100).toInt()
            } else 0

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Presupuesto vs Gasto Real",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Evaluación $periodLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val healthColor = when {
                            executionPct >= 100 -> ExpenseRed
                            executionPct >= 80 -> WarningAmber
                            else -> IncomeGreen
                        }
                        val healthText = when {
                            executionPct >= 100 -> "Excedido"
                            executionPct >= 80 -> "Alerta"
                            else -> "Saludable"
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = healthColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$executionPct% • $healthText",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = healthColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = (executionPct / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = when {
                            executionPct >= 100 -> ExpenseRed
                            executionPct >= 80 -> WarningAmber
                            else -> PrimaryEmerald
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Presupuesto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.2f", periodGeneralLimit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Gastado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.2f", periodExpense)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                        Column {
                            Text("Comprometido", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.2f", periodPendingTotal)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                        }
                        Column {
                            Text("Disponible", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${uiState.currencySymbol}${String.format("%.2f", budgetRemaining)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (budgetRemaining >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }
        }

        // Automatic Donut Chart Canvas for selected period
        item {
            MonthlyExpenseDonutChartCard(
                categoryExpenses = periodCategoryExpenses,
                totalExpense = periodExpense,
                currencySymbol = uiState.currencySymbol
            )
        }

        // Financial Metrics Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Métricas del Periodo ($periodLabel)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val savingRate = if (periodIncome > 0) {
                        (((periodIncome - periodExpense) / periodIncome) * 100).coerceAtLeast(0.0)
                    } else 0.0

                    AnalyticsMetricRow(
                        label = "Tasa de Ahorro del Periodo",
                        value = "${String.format("%.1f", savingRate)}%"
                    )

                    val daysInPeriod = if (selectedPeriodMode == "MONTHLY") 30.0 else 15.0
                    AnalyticsMetricRow(
                        label = "Promedio de Gasto Diario",
                        value = "${uiState.currencySymbol}${String.format("%.2f", periodExpense / daysInPeriod)}"
                    )

                    AnalyticsMetricRow(
                        label = "Categoría Principal",
                        value = periodCategoryExpenses.firstOrNull()?.category ?: "Ninguna"
                    )

                    AnalyticsMetricRow(
                        label = "Registros Realizados",
                        value = "${periodTransactions.size} movimientos"
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
    }
}

@Composable
fun AnalyticsMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
