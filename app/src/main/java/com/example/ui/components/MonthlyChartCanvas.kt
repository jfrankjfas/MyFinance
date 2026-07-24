package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CategoryExpense
import com.example.ui.theme.CategoryPink
import com.example.ui.theme.CategoryPurple
import com.example.ui.theme.CategoryTeal
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PrimaryLightEmerald
import com.example.ui.theme.WarningAmber

val categoryColors = listOf(
    PrimaryEmerald,
    PrimaryLightEmerald,
    InfoBlue,
    CategoryPurple,
    WarningAmber,
    CategoryPink,
    CategoryTeal,
    ExpenseRed
)

fun getCategoryColor(index: Int): Color {
    return categoryColors[index % categoryColors.size]
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyExpenseDonutChartCard(
    categoryExpenses: List<CategoryExpense>,
    totalExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gastos por Categoría este Mes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (categoryExpenses.isEmpty() || totalExpense <= 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aún no hay gastos registrados este mes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val animationProgress = remember { Animatable(0f) }
                LaunchedEffect(categoryExpenses) {
                    animationProgress.animateTo(1f, animationSpec = tween(1000))
                }

                // Donut Chart Canvas
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    Canvas(modifier = Modifier.size(190.dp)) {
                        val strokeWidth = 32.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        var startAngle = -90f

                        categoryExpenses.forEachIndexed { index, item ->
                            val sweepAngle = (item.percentage / 100f) * 360f * animationProgress.value
                            drawArc(
                                color = getCategoryColor(index),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle - 2f, // Small gap between slices
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Center text summary
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Gastos",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol${String.format("%.2f", totalExpense)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Category Legend Breakdown FlowRow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categoryExpenses.forEachIndexed { index, item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(getCategoryColor(index))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.category,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${String.format("%.1f", item.percentage)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
