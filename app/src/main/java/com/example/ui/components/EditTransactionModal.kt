package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.TransactionEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SleekPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionModal(
    transaction: TransactionEntity,
    exchangeRate2: Double = 36.6243,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        category: String,
        type: String,
        note: String,
        timestamp: Long,
        currency: String
    ) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val initialCurrency = remember(transaction) {
        val usdMatch = Regex("""\[(\d+(\.\d+)?) USD\]""").find(transaction.note)
        if (transaction.originalCurrency == "USD" && transaction.originalAmount > 0 && transaction.originalAmount != transaction.amount) "USD"
        else if (usdMatch != null) "USD"
        else "NIO"
    }

    // Determine initial currency and amount
    var selectedCurrency by remember { mutableStateOf(initialCurrency) }
    var titleInput by remember { mutableStateOf(transaction.title) }
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }

    val initialAmountStr = remember(transaction, initialCurrency) {
        val usdMatch = Regex("""\[(\d+(\.\d+)?) USD\]""").find(transaction.note)
        if (initialCurrency == "USD") {
            if (usdMatch != null) {
                usdMatch.groupValues[1]
            } else if (transaction.originalAmount > 0.0 && transaction.originalAmount != transaction.amount) {
                String.format(Locale.US, "%.2f", transaction.originalAmount)
            } else if (transaction.amount > 0.0 && exchangeRate2 > 0) {
                String.format(Locale.US, "%.2f", transaction.amount / exchangeRate2)
            } else {
                "0.00"
            }
        } else {
            // Córdobas NIO
            if (transaction.originalAmount > 0.0 && transaction.originalCurrency == "NIO") {
                String.format(Locale.US, "%.2f", transaction.originalAmount)
            } else {
                String.format(Locale.US, "%.2f", transaction.amount)
            }
        }
    }

    var amountInput by remember { mutableStateOf(initialAmountStr) }

    // Clean note from USD tag for editing
    val cleanedNote = remember(transaction.note) {
        transaction.note.replace(Regex("""\[\d+(\.\d+)? USD\]"""), "").trim()
    }
    var noteInput by remember { mutableStateOf(cleanedNote) }
    var selectedDateMs by remember { mutableLongStateOf(transaction.timestamp) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val sdfDate = remember { SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES")) }

    fun showDatePicker() {
        val c = Calendar.getInstance().apply { timeInMillis = selectedDateMs }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    clear()
                    set(year, month, dayOfMonth, 12, 0, 0)
                }
                selectedDateMs = newCal.timeInMillis
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
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
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Editar Registro",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Type selector: EXPENSE or INCOME
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "EXPENSE",
                        onClick = { selectedType = "EXPENSE" },
                        label = { Text("Gasto") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == "INCOME",
                        onClick = { selectedType = "INCOME" },
                        label = { Text("Ingreso") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title field
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Título / Concepto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Currency selector
                Text(
                    text = "Moneda del Registro",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedCurrency == "NIO",
                        onClick = { selectedCurrency = "NIO" },
                        label = { Text("🇳🇮 Córdobas (C$)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCurrency == "USD",
                        onClick = { selectedCurrency = "USD" },
                        label = { Text("🇺🇸 Dólares (US$)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Amount input
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = {
                        Text(
                            if (selectedCurrency == "USD") "Monto en Dólares ($)"
                            else "Monto en Córdobas (C$)"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                val parsedAmt = amountInput.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
                if (parsedAmt > 0) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val equivText = if (selectedCurrency == "USD") {
                                val inNio = parsedAmt * exchangeRate2
                                "💵 $ ${String.format(Locale.US, "%.2f", parsedAmt)} USD = C$ ${String.format(Locale.US, "%.2f", inNio)} Córdobas (Tasa: ${String.format(Locale.US, "%.4f", exchangeRate2)})"
                            } else {
                                val inUsd = parsedAmt / exchangeRate2
                                "🇳🇮 C$ ${String.format(Locale.US, "%.2f", parsedAmt)} Córdobas = $ ${String.format(Locale.US, "%.2f", inUsd)} USD (Tasa: ${String.format(Locale.US, "%.4f", exchangeRate2)})"
                            }
                            Text(
                                text = equivText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        categoryList.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = sdfDate.format(Date(selectedDateMs)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha del Registro") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar Fecha")
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

                Spacer(modifier = Modifier.height(12.dp))

                // Notes field
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Notas adicionales") },
                    placeholder = { Text("Comentarios o detalles...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Delete and Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar")
                    }

                    Button(
                        onClick = {
                            val cleanAmount = amountInput.replace(',', '.').trim()
                            val validAmount = cleanAmount.toDoubleOrNull() ?: 0.0
                            if (titleInput.isNotBlank() && validAmount > 0) {
                                onSave(
                                    titleInput.trim(),
                                    validAmount,
                                    selectedCategory,
                                    selectedType,
                                    noteInput.trim(),
                                    selectedDateMs,
                                    selectedCurrency
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        enabled = titleInput.isNotBlank() && parsedAmt > 0
                    ) {
                        Text("Guardar Cambios")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar registro?") },
            text = { Text("¿Estás seguro de que deseas eliminar permanentemente '${transaction.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
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
}
