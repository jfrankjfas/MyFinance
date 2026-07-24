package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinanceUiState
import com.example.ui.components.MonthlyExpenseDonutChartCard

@Composable
fun AnalyticsScreen(
    uiState: FinanceUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Column {
                Text(
                    text = "Análisis Financiero",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gráficos y distribución de tus gastos mensuales",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Automatic Monthly Expense Donut Chart Canvas
        item {
            MonthlyExpenseDonutChartCard(
                categoryExpenses = uiState.categoryExpenses,
                totalExpense = uiState.totalExpense,
                currencySymbol = uiState.currencySymbol
            )
        }

        // Financial Summary Metrics Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Resumen de Rendimiento Mensual",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val savingRate = if (uiState.totalIncome > 0) {
                        (((uiState.totalIncome - uiState.totalExpense) / uiState.totalIncome) * 100).coerceAtLeast(0.0)
                    } else 0.0

                    AnalyticsMetricRow(
                        label = "Tasa de Ahorro Estimada",
                        value = "${String.format("%.1f", savingRate)}%"
                    )

                    AnalyticsMetricRow(
                        label = "Promedio de Gasto Diario",
                        value = "${uiState.currencySymbol}${String.format("%.2f", uiState.totalExpense / 30.0)}"
                    )

                    AnalyticsMetricRow(
                        label = "Categoría Principal de Gasto",
                        value = uiState.categoryExpenses.firstOrNull()?.category ?: "Ninguna"
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
}

@Composable
fun AnalyticsMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
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
