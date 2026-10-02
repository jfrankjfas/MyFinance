package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ArchivedPeriodEntity
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveConfirmDialog(
    periodMode: String,
    budgetLimit: Double,
    spentTotal: Double,
    scheduledTotal: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (
        customTitle: String,
        note: String,
        selectedPeriod: String,
        startDateMs: Long?,
        endDateMs: Long?,
        clearPeriodData: Boolean,
        closePermanently: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    val monthStr = sdfMonth.format(Date()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

    var selectedPeriodMode by remember { mutableStateOf(periodMode) }
    var currentStep by remember { mutableIntStateOf(1) } // 1: Setup & Pre-close, 2: Review & Final Lock

    val calNow = Calendar.getInstance()
    val maxDays = calNow.getActualMaximum(Calendar.DAY_OF_MONTH)
    val curYear = calNow.get(Calendar.YEAR)
    val curMonth = calNow.get(Calendar.MONTH)

    val defaultStartMs = remember(selectedPeriodMode) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, curYear)
            set(Calendar.MONTH, curMonth)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            when (selectedPeriodMode) {
                "FORTNIGHT_2" -> set(Calendar.DAY_OF_MONTH, 16)
                else -> set(Calendar.DAY_OF_MONTH, 1)
            }
        }
        c.timeInMillis
    }

    val defaultEndMs = remember(selectedPeriodMode) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, curYear)
            set(Calendar.MONTH, curMonth)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            when (selectedPeriodMode) {
                "FORTNIGHT_1" -> set(Calendar.DAY_OF_MONTH, 15)
                else -> set(Calendar.DAY_OF_MONTH, maxDays)
            }
        }
        c.timeInMillis
    }

    var customStartDateMs by remember { mutableLongStateOf(defaultStartMs) }
    var customEndDateMs by remember { mutableLongStateOf(defaultEndMs) }

    val sdfDateOnly = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val defaultTitle = remember(selectedPeriodMode, customStartDateMs, customEndDateMs) {
        when (selectedPeriodMode) {
            "FORTNIGHT_1" -> "Estado de Cuenta - 1ª Quincena ($monthStr)"
            "FORTNIGHT_2" -> "Estado de Cuenta - 2ª Quincena ($monthStr)"
            "CUSTOM" -> "Estado de Cuenta (${sdfDateOnly.format(Date(customStartDateMs))} - ${sdfDateOnly.format(Date(customEndDateMs))})"
            else -> "Estado de Cuenta - Mes de $monthStr"
        }
    }

    var titleInput by remember(defaultTitle) { mutableStateOf(defaultTitle) }
    var noteInput by remember { mutableStateOf("") }
    var clearPeriodData by remember { mutableStateOf(true) }

    fun pickCustomDate(isStart: Boolean) {
        val initialMs = if (isStart) customStartDateMs else customEndDateMs
        val c = Calendar.getInstance().apply { timeInMillis = initialMs }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newC = Calendar.getInstance().apply {
                    clear()
                    set(year, month, dayOfMonth, if (isStart) 0 else 23, if (isStart) 0 else 59, if (isStart) 0 else 59)
                }
                if (isStart) customStartDateMs = newC.timeInMillis else customEndDateMs = newC.timeInMillis
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (currentStep == 1) Icons.Default.Archive else Icons.Default.Lock,
                contentDescription = null,
                tint = if (currentStep == 1) MaterialTheme.colorScheme.primary else Color(0xFFD32F2F),
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (currentStep == 1) "1️⃣ Definir Periodo (Pre-Cierre)" else "2️⃣ Revisión y Cierre Definitivo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = if (currentStep == 1) "Paso 1 de 2: Elige el periodo para el Estado de Cuenta" else "Paso 2 de 2: Revisa antes del sellado inmutable",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep == 1) {
                    // --- STEP 1: SELECT PERIOD & DATES ---
                    Text(
                        text = "Selecciona el periodo que deseas cerrar o define fechas específicas para tu Estado de Cuenta Oficial:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Period Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedPeriodMode == "MONTHLY",
                                onClick = { selectedPeriodMode = "MONTHLY" },
                                label = { Text("Mensual", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedPeriodMode == "FORTNIGHT_1",
                                onClick = { selectedPeriodMode = "FORTNIGHT_1" },
                                label = { Text("1ª Quincena", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedPeriodMode == "FORTNIGHT_2",
                                onClick = { selectedPeriodMode = "FORTNIGHT_2" },
                                label = { Text("2ª Quincena", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        FilterChip(
                            selected = selectedPeriodMode == "CUSTOM",
                            onClick = { selectedPeriodMode = "CUSTOM" },
                            label = { Text("📅 Rango de Fechas Personalizado", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Custom Date Pickers if selected
                    if (selectedPeriodMode == "CUSTOM") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = sdfDateOnly.format(Date(customStartDateMs)),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Desde", fontSize = 11.sp) },
                                trailingIcon = {
                                    IconButton(onClick = { pickCustomDate(true) }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = sdfDateOnly.format(Date(customEndDateMs)),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Hasta", fontSize = 11.sp) },
                                trailingIcon = {
                                    IconButton(onClick = { pickCustomDate(false) }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Summary Preview Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Presupuesto Asignado:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currencySymbol${String.format(Locale.US, "%.2f", budgetLimit)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gasto Ejecutado:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currencySymbol${String.format(Locale.US, "%.2f", spentTotal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pagos Programados:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currencySymbol${String.format(Locale.US, "%.2f", scheduledTotal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Nombre del Estado de Cuenta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Notas u observaciones (Opcional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // --- STEP 2: PRE-CIERRE REVIEW & FINAL CONFIRMATION ---
                    Surface(
                        color = Color(0xFFFFF9C4),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🟡 MODO PRE-CIERRE (Revisión Final)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFF57F17)
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📄 $titleInput", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("📅 Periodo: ${sdfDateOnly.format(Date(if (selectedPeriodMode == "CUSTOM") customStartDateMs else defaultStartMs))} al ${sdfDateOnly.format(Date(if (selectedPeriodMode == "CUSTOM") customEndDateMs else defaultEndMs))}", fontSize = 12.sp)
                            Divider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Gastado:", fontSize = 12.sp)
                                Text("$currencySymbol${String.format(Locale.US, "%.2f", spentTotal)}", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Compromisos Programados:", fontSize = 12.sp)
                                Text("$currencySymbol${String.format(Locale.US, "%.2f", scheduledTotal)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🔒 Al dar el Cierre Definitivo, este Estado de Cuenta quedará sellado e inmutable. Ya no permitirá editar ni eliminar movimientos de este periodo.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Reset / Clear Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { clearPeriodData = !clearPeriodData }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = clearPeriodData,
                            onCheckedChange = { clearPeriodData = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Limpiar transacciones activas para nuevo periodo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Reinicia los gastos activos a cero. El Estado de Cuenta guardará todo tu detalle respaldado.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentStep == 1) {
                Button(
                    onClick = { currentStep = 2 },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Revisar Pre-Cierre ➡️")
                }
            } else {
                Button(
                    onClick = {
                        val sMs = if (selectedPeriodMode == "CUSTOM") customStartDateMs else defaultStartMs
                        val eMs = if (selectedPeriodMode == "CUSTOM") customEndDateMs else defaultEndMs
                        onConfirm(titleInput, noteInput, selectedPeriodMode, sMs, eMs, clearPeriodData, true)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🔒 Cierre Definitivo")
                }
            }
        },
        dismissButton = {
            if (currentStep == 1) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            } else {
                TextButton(onClick = { currentStep = 1 }) {
                    Text("⬅️ Volver a Ajustar")
                }
            }
        }
    )
}

@Composable
fun ArchivedStatementsListDialog(
    archivedPeriods: List<ArchivedPeriodEntity>,
    onDismiss: () -> Unit,
    onSelectStatement: (ArchivedPeriodEntity) -> Unit,
    onDeleteStatement: (ArchivedPeriodEntity) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = "Estados de Cuenta",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Histórico de presupuestos respaldados",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (archivedPeriods.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "No hay Estados de Cuenta respaldados aún",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Finaliza un periodo de presupuesto para guardarlo aquí.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(archivedPeriods, key = { it.id }) { item ->
                            ArchivedStatementCard(
                                item = item,
                                onClick = { onSelectStatement(item) },
                                onDelete = { onDeleteStatement(item) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
fun ArchivedStatementCard(
    item: ArchivedPeriodEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(Date(item.archivedAt))

    val savingsOrDeficit = item.budgetLimit - item.totalSpent
    val isSavings = savingsOrDeficit >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Archivado el: $formattedDate",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Presupuesto: ${item.currencySymbol}${String.format(Locale.US, "%.2f", item.budgetLimit)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Gastado: ${item.currencySymbol}${String.format(Locale.US, "%.2f", item.totalSpent)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Badge Ahorro / Exceso
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSavings) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSavings) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isSavings) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isSavings) "Ahorro: ${item.currencySymbol}${String.format(Locale.US, "%.2f", savingsOrDeficit)}"
                                else "Exceso: ${item.currencySymbol}${String.format(Locale.US, "%.2f", -savingsOrDeficit)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSavings) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }

                    // Status Badge (Abierto vs Cerrado)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (item.isClosed) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.tertiaryContainer
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (item.isClosed) "🔒 CERRADO" else "🔓 EN EDICIÓN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isClosed) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankStatementDetailDialog(
    item: ArchivedPeriodEntity,
    onDismiss: () -> Unit,
    onCloseStatement: (Long) -> Unit = {},
    onAddTransaction: (periodId: Long, title: String, amount: Double, category: String, type: String, note: String) -> Unit = { _, _, _, _, _, _ -> },
    onEditTransaction: (periodId: Long, txIndex: Int, title: String, amount: Double, category: String, type: String, note: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteTransaction: (periodId: Long, txIndex: Int) -> Unit = { _, _ -> }
) {
    val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy - HH:mm", Locale("es", "ES"))
    val formattedDate = sdf.format(Date(item.archivedAt))

    val savingsOrDeficit = item.budgetLimit - item.totalSpent
    val isSavings = savingsOrDeficit >= 0
    val executionPct = if (item.budgetLimit > 0) ((item.totalSpent / item.budgetLimit) * 100).toInt() else 0

    var showCloseConfirmDialog by remember { mutableStateOf(false) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var editingTxIndex by remember { mutableStateOf<Int?>(null) }

    // Parse Json transactions
    val txList = remember(item.transactionsJson) {
        val list = mutableListOf<StatementTx>()
        try {
            val array = JSONArray(item.transactionsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StatementTx(
                        title = obj.optString("title", "Transacción"),
                        amount = obj.optDouble("amount", 0.0),
                        category = obj.optString("category", "General"),
                        type = obj.optString("type", "EXPENSE"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        note = obj.optString("note", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                    Text(
                        text = "Estado de Cuenta Oficial",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Bank Statement Header Sheet Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "FINANZAS CLARA",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontFamily = FontFamily.Serif
                                        )
                                        Text(
                                            text = "COMPROBANTE DE PRESUPUESTO ARCHIVADO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (item.isClosed) MaterialTheme.colorScheme.primary else Color(0xFFE65100))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (item.isClosed) Icons.Default.CheckCircle else Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = if (item.isClosed) "🔒 CERRADO" else "🔓 EN EDICIÓN",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )

                                Text(
                                    text = "Fecha de Generación: $formattedDate",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (item.note.isNotBlank()) {
                                    Text(
                                        text = "Nota: ${item.note}",
                                        fontSize = 12.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Key Performance Balance Grid
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Resumen Financiero del Periodo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricBox(
                                    label = "Presupuesto",
                                    amount = "${item.currencySymbol}${String.format(Locale.US, "%.2f", item.budgetLimit)}",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    label = "Gasto Real",
                                    amount = "${item.currencySymbol}${String.format(Locale.US, "%.2f", item.totalSpent)}",
                                    color = Color(0xFFD32F2F),
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    label = if (isSavings) "Ahorro" else "Exceso",
                                    amount = "${item.currencySymbol}${String.format(Locale.US, "%.2f", Math.abs(savingsOrDeficit))}",
                                    color = if (isSavings) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Execution Bar
                            Column(modifier = Modifier.padding(top = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ejecución del Presupuesto", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$executionPct%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = (executionPct / 100f).coerceIn(0f, 1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (executionPct > 100) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Detailed Movement Ledger
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Detalle de Movimientos (${txList.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                if (!item.isClosed) {
                                    TextButton(onClick = { showAddTxDialog = true }) {
                                        Text("+ Agregar Movimiento", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (txList.isEmpty()) {
                                Text(
                                    text = "No hay transacciones registradas en este estado de cuenta.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    txList.forEachIndexed { index, tx ->
                                        StatementTxRow(
                                            tx = tx,
                                            currencySymbol = item.currencySymbol,
                                            isClosed = item.isClosed,
                                            onEdit = { editingTxIndex = index },
                                            onDelete = { onDeleteTransaction(item.id, index) }
                                        )
                                        if (index < txList.size - 1) {
                                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                if (!item.isClosed) {
                    Button(
                        onClick = { showCloseConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔒 Realizar Cierre Definitivo de Estado de Cuenta")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "🔒 Estado de Cuenta Cerrado Oficialmente. Modo Solo Lectura.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar Vista")
                }
            }
        }
    }

    // Confirmation Alert for Closing EC
    if (showCloseConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCloseConfirmDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Cerrar Estado de Cuenta Definitivamente?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Al realizar el cierre, el Estado de Cuenta quedará bloqueado en modo de solo lectura. Ya no podrás agregar, editar ni eliminar transacciones en este periodo.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCloseConfirmDialog = false
                        onCloseStatement(item.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sí, Cerrar Definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Add Transaction Dialog for EC
    if (showAddTxDialog) {
        StatementTxDialog(
            currencySymbol = item.currencySymbol,
            initialTx = null,
            onDismiss = { showAddTxDialog = false },
            onConfirm = { title, amount, category, type, note ->
                onAddTransaction(item.id, title, amount, category, type, note)
                showAddTxDialog = false
            }
        )
    }

    // Edit Transaction Dialog for EC
    editingTxIndex?.let { index ->
        val currentTx = txList.getOrNull(index)
        if (currentTx != null) {
            StatementTxDialog(
                currencySymbol = item.currencySymbol,
                initialTx = currentTx,
                onDismiss = { editingTxIndex = null },
                onConfirm = { title, amount, category, type, note ->
                    onEditTransaction(item.id, index, title, amount, category, type, note)
                    editingTxIndex = null
                }
            )
        }
    }
}

@Composable
fun MetricBox(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(amount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun StatementTxRow(
    tx: StatementTx,
    currencySymbol: String,
    isClosed: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateStr = sdf.format(Date(tx.timestamp))
    val isExpense = tx.type == "EXPENSE"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("${tx.category} • $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (tx.note.isNotBlank()) {
                Text(tx.note, fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.outline)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${if (isExpense) "-" else "+"}$currencySymbol${String.format(Locale.US, "%.2f", tx.amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isExpense) Color(0xFFD32F2F) else Color(0xFF2E7D32)
            )

            if (!isClosed) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar movimiento",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar movimiento",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementTxDialog(
    currencySymbol: String,
    initialTx: StatementTx?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, category: String, type: String, note: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTx?.title ?: "") }
    var amountText by remember { mutableStateOf(initialTx?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var selectedCategory by remember { mutableStateOf(initialTx?.category ?: "Alimentación") }
    var type by remember { mutableStateOf(initialTx?.type ?: "EXPENSE") }
    var note by remember { mutableStateOf(initialTx?.note ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Alimentación", "Transporte", "Servicios", "Entretenimiento", "Salud", "Educación", "Otros")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTx == null) "Agregar Movimiento al EC" else "Editar Movimiento del EC", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { type = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "EXPENSE") Color(0xFFD32F2F) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gasto", color = if (type == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = { type = "INCOME" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "INCOME") Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ingreso", color = if (type == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Concepto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (Opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onConfirm(title, amount, selectedCategory, type, note)
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

data class StatementTx(
    val title: String,
    val amount: Double,
    val category: String,
    val type: String,
    val timestamp: Long,
    val note: String
)
