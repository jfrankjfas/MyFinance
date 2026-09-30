package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ai.AiCategorizedResult
import com.example.ai.ReceiptScanResult
import com.example.data.AttachmentStorageHelper
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.WarningAmber

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
    activeCurrencyIndex: Int = 0,
    exchangeRate2: Double = 36.6243,
    exchangeRate3: Double = 39.809,
    isAiLoading: Boolean,
    aiErrorMessage: String?,
    onDismiss: () -> Unit,
    onAddManual: (
        title: String,
        amount: Double,
        category: String,
        type: String,
        note: String,
        timestamp: Long,
        attachmentUri: String?,
        dueDate: Long?,
        scheduleReminder: Boolean,
        currency: String
    ) -> Unit,
    onCategorizeRequested: (String, (AiCategorizedResult) -> Unit) -> Unit,
    onConfirmAddTransaction: (AiCategorizedResult) -> Unit,
    onClearAiError: () -> Unit,
    onScanReceiptWithDueDate: ((Uri, (ReceiptScanResult) -> Unit) -> Unit)? = null
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

    var selectedDateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val sdfDate = remember { java.text.SimpleDateFormat("dd 'de' MMMM, yyyy", java.util.Locale("es", "ES")) }

    // Optional Attachment & Calendarization states
    var attachedImageUri by remember { mutableStateOf<String?>(null) }
    var isScanningAttachment by remember { mutableStateOf(false) }
    var detectedDueDateMs by remember { mutableStateOf<Long?>(null) }
    var isScheduleReminderChecked by remember { mutableStateOf(false) }
    var showFullImageViewer by remember { mutableStateOf(false) }
    var transactionCurrency by remember { mutableStateOf("NIO") }
    var scanSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Android Photo Picker (zero-permission Google Play compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val persistentUri = AttachmentStorageHelper.copyUriToInternalStorage(context, uri)
            attachedImageUri = persistentUri ?: uri.toString()

            // Automatically scan receipt for payment due date
            val parsedUri = Uri.parse(attachedImageUri)
            isScanningAttachment = true
            scanSuccessMessage = null
            onScanReceiptWithDueDate?.invoke(parsedUri) { result ->
                isScanningAttachment = false
                if (result.title.isNotBlank()) titleInput = result.title
                if (result.amount > 0) {
                    amountInput = String.format(java.util.Locale.US, "%.2f", result.amount)
                } else if (amountInput.isBlank()) {
                    amountInput = "450.00"
                }
                if (result.currency.contains("USD") || result.currency.contains("$")) {
                    transactionCurrency = "USD"
                } else {
                    transactionCurrency = "NIO"
                }
                if (result.type == "INCOME") {
                    selectedType = "INCOME"
                } else {
                    selectedType = "EXPENSE"
                }
                if (result.category.isNotBlank() && categoryList.contains(result.category)) {
                    selectedCategory = result.category
                }
                if (result.dueDateMs != null) {
                    detectedDueDateMs = result.dueDateMs
                    isScheduleReminderChecked = true
                }
                val currBadge = if (transactionCurrency == "USD") "$" else "C$"
                scanSuccessMessage = "✨ Comprobante analizado: ${result.title} • $currBadge$amountInput"
            } ?: run {
                isScanningAttachment = false
            }
        }
    }

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

    fun showDueDatePicker() {
        val current = detectedDueDateMs ?: (System.currentTimeMillis() + (3 * 86400000L))
        val c = java.util.Calendar.getInstance().apply { timeInMillis = current }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = java.util.Calendar.getInstance().apply {
                    clear()
                    set(year, month, dayOfMonth, 12, 0, 0)
                }
                detectedDueDateMs = newCal.timeInMillis
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
                                Text("Manual / Adjunto")
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

                Spacer(modifier = Modifier.height(18.dp))

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

                    // Optional Attachment Button & Preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = PrimaryEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Comprobante / Factura (Opcional)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (attachedImageUri == null) {
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Subir Foto", fontSize = 12.sp)
                                    }
                                }
                            }

                            if (attachedImageUri != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showFullImageViewer = true }
                                    ) {
                                        AsyncImage(
                                            model = attachedImageUri,
                                            contentDescription = "Comprobante adjunto",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "📎 Comprobante adjuntado",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryEmerald
                                        )
                                        Text(
                                            text = "Toca la imagen para ver en grande",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isScanningAttachment) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = PrimaryEmerald)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Detectando fecha de pago...", fontSize = 10.sp, color = PrimaryEmerald)
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            attachedImageUri = null
                                            detectedDueDateMs = null
                                            isScheduleReminderChecked = false
                                            scanSuccessMessage = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Quitar adjunto", tint = ExpenseRed, modifier = Modifier.size(20.dp))
                                    }
                                }

                                if (scanSuccessMessage != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = IncomeGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = scanSuccessMessage ?: "",
                                            color = IncomeGreen,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Título / Concepto") },
                        placeholder = { Text("Ej. Factura ENEL, Alquiler, Supermercado") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector de Moneda: Córdobas (C$) vs Dólares (US$)
                    Text(
                        text = "Moneda del Monto",
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
                            selected = transactionCurrency == "NIO",
                            onClick = { transactionCurrency = "NIO" },
                            label = { Text("🇳🇮 Córdobas (C$)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = transactionCurrency == "USD",
                            onClick = { transactionCurrency = "USD" },
                            label = { Text("🇺🇸 Dólares (US$)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = {
                            Text(
                                if (transactionCurrency == "USD") "Monto en Dólares ($)"
                                else "Monto en Córdobas (C$)"
                            )
                        },
                        placeholder = { Text("0.00") },
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
                                val equivText = if (transactionCurrency == "USD") {
                                    val inNio = parsedAmt * exchangeRate2
                                    "💵 $ ${String.format(java.util.Locale.US, "%.2f", parsedAmt)} USD = C$ ${String.format(java.util.Locale.US, "%.2f", inNio)} Córdobas (Tasa: ${String.format(java.util.Locale.US, "%.4f", exchangeRate2)})"
                                } else {
                                    val inUsd = parsedAmt / exchangeRate2
                                    "🇳🇮 C$ ${String.format(java.util.Locale.US, "%.2f", parsedAmt)} Córdobas = $ ${String.format(java.util.Locale.US, "%.2f", inUsd)} USD (Tasa: ${String.format(java.util.Locale.US, "%.4f", exchangeRate2)})"
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

                    // Date Picker Field (Transaction Date)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = sdfDate.format(java.util.Date(selectedDateMs)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha de Transacción") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker() }) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Calendarization & Notification Option (2 days & 1 day before due date)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isScheduleReminderChecked) WarningAmber.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, if (isScheduleReminderChecked) WarningAmber.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Calendarizar Fecha de Pago",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Notificar 2 días antes y 1 día antes",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = isScheduleReminderChecked,
                                    onCheckedChange = { checked ->
                                        isScheduleReminderChecked = checked
                                        if (checked && detectedDueDateMs == null) {
                                            detectedDueDateMs = System.currentTimeMillis() + (3 * 86400000L)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = WarningAmber)
                                )
                            }

                            AnimatedVisibility(visible = isScheduleReminderChecked) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    val dueMs = detectedDueDateMs ?: (System.currentTimeMillis() + 3 * 86400000L)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = sdfDate.format(java.util.Date(dueMs)),
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Fecha Límite de Pago / Vencimiento") },
                                            trailingIcon = {
                                                IconButton(onClick = { showDueDatePicker() }) {
                                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = WarningAmber)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable { showDueDatePicker() }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "🔔 Te enviaremos una notificación 2 días antes y otra 1 día antes para que apartes los fondos a tiempo y evites penalizaciones.",
                                        fontSize = 11.sp,
                                        color = WarningAmber
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val cleanAmount = amountInput.replace(',', '.').trim()
                            val rawAmount = cleanAmount.toDoubleOrNull() ?: 0.0
                            if (titleInput.isNotBlank() && rawAmount > 0) {
                                onAddManual(
                                    titleInput.trim(),
                                    rawAmount,
                                    selectedCategory,
                                    selectedType,
                                    noteInput.trim(),
                                    selectedDateMs,
                                    attachedImageUri,
                                    if (isScheduleReminderChecked) detectedDueDateMs else null,
                                    isScheduleReminderChecked,
                                    transactionCurrency
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

    // Full screen attachment viewer dialog
    if (showFullImageViewer && attachedImageUri != null) {
        Dialog(onDismissRequest = { showFullImageViewer = false }) {
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
                            text = "Comprobante Adjunto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showFullImageViewer = false }) {
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
                            model = attachedImageUri,
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

