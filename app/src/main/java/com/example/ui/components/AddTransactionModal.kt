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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ai.AiCategorizedResult
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryEmerald

val categoryList = listOf(
    "Alimentación",
    "Transporte",
    "Entretenimiento",
    "Servicios",
    "Salud",
    "Educación",
    "Compras",
    "Sueldo",
    "Inversión",
    "Otro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    currencySymbol: String,
    isAiLoading: Boolean,
    aiErrorMessage: String?,
    onDismiss: () -> Unit,
    onAddManual: (title: String, amount: Double, category: String, type: String, note: String, timestamp: Long) -> Unit,
    onCategorizeRequested: (String, (AiCategorizedResult) -> Unit) -> Unit,
    onConfirmAddTransaction: (AiCategorizedResult) -> Unit,
    onClearAiError: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Manual, 1: AI Assistant

    // Manual Form states
    var titleInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var selectedCategory by remember { mutableStateOf("Alimentación") }
    var noteInput by remember { mutableStateOf("") }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    var selectedDateMs by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    val sdfDate = remember { java.text.SimpleDateFormat("dd 'de' MMMM, yyyy", java.util.Locale("es", "ES")) }

    fun showDatePicker() {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = selectedDateMs }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = java.util.Calendar.getInstance().apply {
                    clear()
                    set(year, month, dayOfMonth, 12, 0, 0)
                }
                selectedDateMs = newCal.timeInMillis
            },
            c.get(java.util.Calendar.YEAR),
            c.get(java.util.Calendar.MONTH),
            c.get(java.util.Calendar.DAY_OF_MONTH)
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
                    Text(
                        text = "Nueva Transacción",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Manual vs IA
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manual")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Asistente IA")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedTab == 0) {
                    // --- MANUAL FORM ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == "EXPENSE",
                            onClick = {
                                selectedType = "EXPENSE"
                                if (selectedCategory == "Sueldo") selectedCategory = "Alimentación"
                            },
                            label = { Text("🔴 Gasto") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedType == "INCOME",
                            onClick = {
                                selectedType = "INCOME"
                                selectedCategory = "Sueldo"
                            },
                            label = { Text("🟢 Ingreso") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Título / Concepto") },
                        placeholder = { Text("Ej. Supermercado, Sueldo, Taxi") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Monto ($currencySymbol)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

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
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false }
                        ) {
                            categoryList.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Nota (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Picker Field
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = sdfDate.format(java.util.Date(selectedDateMs)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha de Transacción") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker() }) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.CalendarToday,
                                        contentDescription = "Seleccionar fecha",
                                        tint = PrimaryEmerald
                                    )
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

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val cleanAmount = amountInput.replace(',', '.').trim()
                            val parsedAmount = cleanAmount.toDoubleOrNull() ?: 0.0
                            if (titleInput.isNotBlank() && parsedAmount > 0) {
                                onAddManual(
                                    titleInput.trim(),
                                    parsedAmount,
                                    selectedCategory,
                                    selectedType,
                                    noteInput.trim(),
                                    selectedDateMs
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        enabled = titleInput.isNotBlank() && (amountInput.replace(',', '.').trim().toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Guardar Transacción")
                    }
                } else {
                    // --- AI ASSISTANT CARD ---
                    AiCategorizerCard(
                        isAiLoading = isAiLoading,
                        errorMessage = aiErrorMessage,
                        currencySymbol = currencySymbol,
                        onCategorizeRequested = onCategorizeRequested,
                        onConfirmAddTransaction = { res ->
                            onConfirmAddTransaction(res)
                            onDismiss()
                        },
                        onClearError = onClearAiError
                    )
                }
            }
        }
    }
}
