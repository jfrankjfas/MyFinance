package com.example.ui.components

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ExtraordinaryFundEntity
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FundAllocation(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val note: String,
    val timestamp: Long
)

@Composable
fun ExtraordinaryFundsSection(
    funds: List<ExtraordinaryFundEntity>,
    currencySymbol: String,
    onAddFund: (title: String, totalAmount: Double, note: String) -> Unit,
    onUpdateFund: (ExtraordinaryFundEntity) -> Unit,
    onDeleteFund: (ExtraordinaryFundEntity) -> Unit,
    onAddAllocation: (fundId: Long, title: String, amount: Double, category: String, note: String) -> Unit,
    onEditAllocation: (fundId: Long, allocationId: String, title: String, amount: Double, category: String, note: String) -> Unit,
    onDeleteAllocation: (fundId: Long, allocationId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateFundDialog by remember { mutableStateOf(false) }
    var selectedFundForDetail by remember { mutableStateOf<ExtraordinaryFundEntity?>(null) }
    var editingFund by remember { mutableStateOf<ExtraordinaryFundEntity?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Ingresos Extraordinarios",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Presupuestos sin fecha límite fija",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(
                onClick = { showCreateFundDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo Fondo", fontWeight = FontWeight.Bold)
            }
        }

        if (funds.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "No tienes presupuestos de ingresos extraordinarios",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Registra bonos, ventas u otros ingresos extras para ir asignando su gasto progresivamente sin prisas por fecha.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            funds.forEach { fund ->
                ExtraordinaryFundCard(
                    fund = fund,
                    currencySymbol = currencySymbol,
                    onManageAllocations = { selectedFundForDetail = fund },
                    onEdit = { editingFund = fund },
                    onDelete = { onDeleteFund(fund) }
                )
            }
        }
    }

    // Add Fund Dialog
    if (showCreateFundDialog) {
        AddEditFundDialog(
            currencySymbol = currencySymbol,
            initialFund = null,
            onDismiss = { showCreateFundDialog = false },
            onConfirm = { title, amount, note ->
                onAddFund(title, amount, note)
                showCreateFundDialog = false
            }
        )
    }

    // Edit Fund Dialog
    editingFund?.let { fund ->
        AddEditFundDialog(
            currencySymbol = currencySymbol,
            initialFund = fund,
            onDismiss = { editingFund = null },
            onConfirm = { title, amount, note ->
                onUpdateFund(fund.copy(title = title, totalAmount = amount, note = note))
                editingFund = null
            }
        )
    }

    // Detail Modal
    selectedFundForDetail?.let { fund ->
        // Keep active reference updated from state
        val liveFund = funds.find { it.id == fund.id } ?: fund
        ExtraordinaryFundDetailDialog(
            fund = liveFund,
            currencySymbol = currencySymbol,
            onDismiss = { selectedFundForDetail = null },
            onAddAllocation = { title, amount, category, note ->
                onAddAllocation(liveFund.id, title, amount, category, note)
            },
            onEditAllocation = { allocId, title, amount, category, note ->
                onEditAllocation(liveFund.id, allocId, title, amount, category, note)
            },
            onDeleteAllocation = { allocId ->
                onDeleteAllocation(liveFund.id, allocId)
            }
        )
    }
}

@Composable
fun ExtraordinaryFundCard(
    fund: ExtraordinaryFundEntity,
    currencySymbol: String,
    onManageAllocations: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val allocations = remember(fund.allocationsJson) {
        parseAllocations(fund.allocationsJson)
    }
    val totalAllocated = allocations.sumOf { it.amount }
    val remainingBalance = fund.totalAmount - totalAllocated
    val allocatedPct = if (fund.totalAmount > 0) ((totalAllocated / fund.totalAmount) * 100).toInt() else 0

    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateStr = sdf.format(Date(fund.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fund.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Registrado el: $dateStr • ${allocations.size} Asignaciones",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (fund.note.isNotBlank()) {
                Text(
                    text = "Nota: ${fund.note}",
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricColumn(
                    label = "Fondo Total",
                    amount = "$currencySymbol${String.format(Locale.US, "%.2f", fund.totalAmount)}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricColumn(
                    label = "Ocupado / Asignado",
                    amount = "$currencySymbol${String.format(Locale.US, "%.2f", totalAllocated)}",
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f)
                )
                MetricColumn(
                    label = "Disponible",
                    amount = "$currencySymbol${String.format(Locale.US, "%.2f", remainingBalance)}",
                    color = if (remainingBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.weight(1f)
                )
            }

            // Allocation Progress
            Column(modifier = Modifier.padding(top = 2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Asignación de Presupuesto", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$allocatedPct%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (allocatedPct / 100f).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (allocatedPct > 100) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onManageAllocations,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = "🎯 Gestionar Asignaciones (${allocations.size})",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun MetricColumn(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(amount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraordinaryFundDetailDialog(
    fund: ExtraordinaryFundEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAddAllocation: (title: String, amount: Double, category: String, note: String) -> Unit,
    onEditAllocation: (allocationId: String, title: String, amount: Double, category: String, note: String) -> Unit,
    onDeleteAllocation: (allocationId: String) -> Unit
) {
    val allocations = remember(fund.allocationsJson) { parseAllocations(fund.allocationsJson) }
    val totalAllocated = allocations.sumOf { it.amount }
    val remainingBalance = fund.totalAmount - totalAllocated

    var showAddAllocationDialog by remember { mutableStateOf(false) }
    var editingAllocation by remember { mutableStateOf<FundAllocation?>(null) }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                    Text(
                        text = fund.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Summary Header Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ingreso Extraordinario Total:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol${String.format(Locale.US, "%.2f", fund.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Asignado / Ocupado:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol${String.format(Locale.US, "%.2f", totalAllocated)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Saldo Restante por Asignar:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currencySymbol${String.format(Locale.US, "%.2f", remainingBalance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (remainingBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lista de Asignaciones (${allocations.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Button(
                        onClick = { showAddAllocationDialog = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Asignar Gasto", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (allocations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no has registrado compras o pagos asignados a este ingreso extraordinario.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allocations, key = { it.id }) { alloc ->
                            AllocationItemRow(
                                alloc = alloc,
                                currencySymbol = currencySymbol,
                                onEdit = { editingAllocation = alloc },
                                onDelete = { onDeleteAllocation(alloc.id) }
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
                    Text("Guardar y Cerrar")
                }
            }
        }
    }

    // Add Allocation Dialog
    if (showAddAllocationDialog) {
        AddEditAllocationDialog(
            currencySymbol = currencySymbol,
            initialAllocation = null,
            onDismiss = { showAddAllocationDialog = false },
            onConfirm = { title, amount, category, note ->
                onAddAllocation(title, amount, category, note)
                showAddAllocationDialog = false
            }
        )
    }

    // Edit Allocation Dialog
    editingAllocation?.let { alloc ->
        AddEditAllocationDialog(
            currencySymbol = currencySymbol,
            initialAllocation = alloc,
            onDismiss = { editingAllocation = null },
            onConfirm = { title, amount, category, note ->
                onEditAllocation(alloc.id, title, amount, category, note)
                editingAllocation = null
            }
        )
    }
}

@Composable
fun AllocationItemRow(
    alloc: FundAllocation,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(alloc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Categoría: ${alloc.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (alloc.note.isNotBlank()) {
                    Text(alloc.note, fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.outline)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%.2f", alloc.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFD32F2F)
                )

                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AddEditFundDialog(
    currencySymbol: String,
    initialFund: ExtraordinaryFundEntity?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, note: String) -> Unit
) {
    var title by remember { mutableStateOf(initialFund?.title ?: "") }
    var amountText by remember { mutableStateOf(initialFund?.totalAmount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var note by remember { mutableStateOf(initialFund?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialFund == null) "Nuevo Ingreso Extraordinario" else "Editar Ingreso Extraordinario", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título (Ej. Bono Anual, Venta Terreno, Freelance)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto del Ingreso ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notas u observaciones (Opcional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onConfirm(title, amount, note)
                    }
                }
            ) {
                Text("Guardar Fondo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAllocationDialog(
    currencySymbol: String,
    initialAllocation: FundAllocation?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, category: String, note: String) -> Unit
) {
    var title by remember { mutableStateOf(initialAllocation?.title ?: "") }
    var amountText by remember { mutableStateOf(initialAllocation?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var selectedCategory by remember { mutableStateOf(initialAllocation?.category ?: "Compras") }
    var note by remember { mutableStateOf(initialAllocation?.note ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Compras", "Pagos", "Servicios", "Tecnología", "Hogar", "Viajes", "Otros")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialAllocation == null) "Nueva Asignación de Fondo" else "Editar Asignación", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("¿En qué se ocupó? (Ej. Pago de Laptop, TV, Reparación)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto Asignado ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                        onConfirm(title, amount, selectedCategory, note)
                    }
                }
            ) {
                Text("Guardar Asignación")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun parseAllocations(jsonStr: String): List<FundAllocation> {
    val list = mutableListOf<FundAllocation>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                FundAllocation(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.optString("title", "Asignación"),
                    amount = obj.optDouble("amount", 0.0),
                    category = obj.optString("category", "Otros"),
                    note = obj.optString("note", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
