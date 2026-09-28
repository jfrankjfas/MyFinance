package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ScheduledExpenseEntity
import com.example.ui.FinanceUiState
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinancialRescueModal(
    uiState: FinanceUiState,
    onDismiss: () -> Unit,
    onGoToScheduledPayments: () -> Unit = {},
    onResetToZero: () -> Unit = {}
) {
    val totalExpenditure = uiState.totalExpense + uiState.periodPendingExpensesTotal
    val deficit = (totalExpenditure - uiState.totalIncome).coerceAtLeast(0.0)
    val currency = uiState.currencySymbol

    // Interactive emergency checklist
    val checklistItems = remember {
        mutableStateListOf(
            "Congelar de inmediato salidas a restaurantes, delivery y delivery apps." to false,
            "Pausar temporalmente suscripciones no esenciales (Netflix, Spotify, etc.)." to false,
            "Cero compras con tarjetas de crédito para no inflar intereses moratorios." to false,
            "Pagar estrictamente lo que vence en 24h a 48h para evitar cortes y multas." to false,
            "Revisar despensa y armar menú semanal estricto con compras esenciales." to false,
            "Llamar a acreedores para solicitar reestructuración o congelamiento de mora." to false
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CrisisAlert,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Plan Rescate Anticrisis",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Estrategia para frenar la quiebra",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Urgent Deficit Diagnosis Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ExpenseRed.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.5.dp, ExpenseRed.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DIAGNÓSTICO: DÉFICIT FINANCIERO ACTIVO",
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Tus Ingresos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "$currency${String.format(Locale.US, "%.2f", uiState.totalIncome)}",
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen,
                                    fontSize = 16.sp
                                )
                            }
                            Column {
                                Text("Gastos + Pagos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "$currency${String.format(Locale.US, "%.2f", totalExpenditure)}",
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed,
                                    fontSize = 16.sp
                                )
                            }
                            Column {
                                Text("Déficit Mensual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "-$currency${String.format(Locale.US, "%.2f", deficit)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ExpenseRed,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (deficit > 0) {
                                "⚠️ Tus gastos superan tus ingresos por $currency${String.format(Locale.US, "%.2f", deficit)}. Para salir de la quiebra debemos blindar pagos vitales y cortar fugas de dinero de inmediato."
                            } else {
                                "💡 Tienes la oportunidad de reconstruir tus finanzas desde cero manteniendo tus gastos estrictamente por debajo de tus ingresos."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // The 3-Tier Survival Hierarchy
                Text(
                    text = "Regla de Triaje Financiero (Qué pagar primero)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tier 1: Vitales Innegociables
                SurvivalTierCard(
                    tierNumber = "1",
                    title = "Gastos Vitales Innegociables",
                    subtitle = "Alimentación básica, Techo/Alquiler, Servicios esenciales (Luz/Agua)",
                    description = "Nadie puede vivir sin techo ni comida. Estos fondos se separan primero y se reducen a lo estrictamente necesario sin lujos.",
                    color = PrimaryEmerald
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tier 2: Deudas Críticas y Próximos Vencimientos
                SurvivalTierCard(
                    tierNumber = "2",
                    title = "Pagos con Vencimiento Inminente (1 a 2 Días)",
                    subtitle = "Con recordatorios automáticos 2 días y 1 día antes",
                    description = "Prioriza pagar las cuentas que vencen en las próximas 48 horas para evitar multas moratorias punitivas que inflan tu deuda.",
                    color = WarningAmber
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tier 3: Fugas Prescindibles
                SurvivalTierCard(
                    tierNumber = "3",
                    title = "Fugas Prescindibles (CONGELAR YA)",
                    subtitle = "Suscripciones, delivery, restaurantes, ocio y compras impulso",
                    description = "En quiebra cada peso cuenta. Congela temporalmente estos gastos hasta que tus ingresos superen tus compromisos.",
                    color = ExpenseRed
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Emergency Checklist
                Text(
                    text = "Plan de Choque Inmediato",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Marca las acciones que has tomado hoy:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                checklistItems.forEachIndexed { index, item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (item.second) PrimaryEmerald.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (item.second) PrimaryEmerald.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = item.second,
                                onCheckedChange = { checked ->
                                    checklistItems[index] = item.first to checked
                                },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.first,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (item.second) FontWeight.Bold else FontWeight.Normal,
                                color = if (item.second) PrimaryEmerald else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Button(
                    onClick = {
                        onDismiss()
                        onGoToScheduledPayments()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Revisar Calendario de Pagos y Alertas")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Entendido, Continuar Organizando")
                }
            }
        }
    }
}

@Composable
fun SurvivalTierCard(
    tierNumber: String,
    title: String,
    subtitle: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.07f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tierNumber,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = color
                )
                Text(
                    text = subtitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
