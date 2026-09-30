package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Warning
import com.example.ui.components.FinancialRescueModal
import com.example.ui.components.EditTransactionModal
import com.example.ui.theme.WarningAmber
import com.example.data.entity.TransactionEntity
import com.example.ui.FinanceUiState
import com.example.ui.components.BudgetAlertBanner
import com.example.ui.components.MainBalanceHeaderCard
import com.example.ui.theme.CategoryPurple
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.ui.components.TopCurrencyHeader
import com.example.ui.theme.SleekPrimary

@Composable
fun HomeScreen(
    uiState: FinanceUiState,
    onAddTransactionClicked: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditTransaction: (TransactionEntity, String, Double, String, String, String, Long, String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onCurrencySelected: (String) -> Unit,
    onCurrencyIndexSelected: (Int) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onGoToScheduledPayments: () -> Unit = {},
    onResetToZero: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showRescueModal by remember { mutableStateOf(false) }
    var viewerImageUri by remember { mutableStateOf<String?>(null) }
    var selectedTransactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    val generalProgress = uiState.budgetProgresses.find { it.category == "Presupuesto Total Mensual" }

    val filteredTransactions = uiState.transactions.filter { tx ->
        tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Top Header with Gmail account profile and 3 Currency Parameter Switcher
            item {
                TopCurrencyHeader(
                    userName = uiState.userName,
                    userEmail = uiState.userEmail,
                    isDriveConnected = uiState.isGoogleDriveConnected,
                    activeCurrencySymbol = uiState.currencySymbol,
                    activeCurrencyIndex = uiState.activeCurrencyIndex,
                    exchangeRate2 = uiState.exchangeRate2,
                    exchangeRate3 = uiState.exchangeRate3,
                    favoriteCurrencies = uiState.favoriteCurrencies,
                    onCurrencyIndexSelected = onCurrencyIndexSelected,
                    onCurrencySelected = onCurrencySelected,
                    onProfileClick = onProfileClick
                )
            }

            // Real-time Firebase Online indicator
            item {
                val status = uiState.firebaseSyncStatus
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (status.isConnected) PrimaryEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (status.isConnected) PrimaryEmerald.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (status.isConnected) PrimaryEmerald else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (status.isConnected) "Firebase Online • Sincronización en vivo" else status.syncStatusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (status.isConnected) PrimaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Nube activa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Financial Bankruptcy / Crisis Rescue Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRescueModal = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isBankruptcyAlert) ExpenseRed.copy(alpha = 0.09f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(1.5.dp, if (uiState.isBankruptcyAlert) ExpenseRed.copy(alpha = 0.45f) else PrimaryEmerald.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isBankruptcyAlert) ExpenseRed.copy(alpha = 0.18f) else PrimaryEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CrisisAlert,
                                contentDescription = null,
                                tint = if (uiState.isBankruptcyAlert) ExpenseRed else PrimaryEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (uiState.isBankruptcyAlert) "🚨 SOS FINANCIERO: ESTADO EN QUIEBRA" else "🛡️ PLAN DE RESCATE FINANCIERO",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = if (uiState.isBankruptcyAlert) ExpenseRed else PrimaryEmerald
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (uiState.monthlyDeficit > 0) {
                                    "Déficit: -${uiState.currencySymbol}${String.format(Locale.US, "%.2f", uiState.monthlyDeficit)}. Toca para ver el plan de triaje y rescate."
                                } else {
                                    "Organiza pagos prioritarios, evita intereses y recorta fugas."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { showRescueModal = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isBankruptcyAlert) ExpenseRed else PrimaryEmerald
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Abrir Plan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Main Balance Card
            item {
                MainBalanceHeaderCard(
                    totalBalance = uiState.totalBalance,
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense,
                    currencySymbol = uiState.currencySymbol
                )
            }

            // Budget Exceeded / Warning Banner
            item {
                generalProgress?.let { prog ->
                    BudgetAlertBanner(
                        isExceeded = prog.isExceeded,
                        isWarning = prog.isWarning,
                        message = if (prog.isExceeded) {
                            "Has gastado ${uiState.currencySymbol}${String.format("%.2f", prog.spentAmount)} de tu límite mensual de ${uiState.currencySymbol}${String.format("%.2f", prog.limitAmount)} (${prog.percentage}%)."
                        } else {
                            "Llevas el ${prog.percentage}% de tu presupuesto mensual configurado."
                        }
                    )
                }
            }

            // Search Transactions
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nombre, categoría...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald
                    ),
                    singleLine = true
                )
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Movimientos Recientes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredTransactions.size} registros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Transaction Items
            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!uiState.isGoogleDriveConnected) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!uiState.isGoogleDriveConnected) {
                                Text(
                                    text = "🔒 Sesión no iniciada",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Al cerrar sesión, todos los registros se retiraron de la app por seguridad y privacidad. Inicia sesión con tu cuenta Google para descargar tus datos vinculados a Gmail.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onProfileClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Iniciar Sesión con Google", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "Sin movimientos registrados",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tus registros están vinculados a ${uiState.userEmail}. Presiona el botón '+' para agregar un nuevo movimiento.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionRowItem(
                        transaction = tx,
                        currencySymbol = uiState.currencySymbol,
                        onEdit = { selectedTransactionToEdit = uiState.rawTransactions.find { it.id == tx.id } ?: tx },
                        onDelete = { onDeleteTransaction(tx) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // FAB to add new transaction
        FloatingActionButton(
            onClick = onAddTransactionClicked,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = SleekPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nueva Transacción", modifier = Modifier.size(28.dp))
        }
    }

    // Modal de Plan de Rescate Financiero y Triaje
    if (showRescueModal) {
        FinancialRescueModal(
            uiState = uiState,
            onDismiss = { showRescueModal = false },
            onGoToScheduledPayments = {
                showRescueModal = false
                onGoToScheduledPayments()
            },
            onResetToZero = {
                showRescueModal = false
                onResetToZero()
            }
        )
    }

    // Modal de vista ampliada de comprobante / recibo
    viewerImageUri?.let { uri ->
        Dialog(onDismissRequest = { viewerImageUri = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Comprobante",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewerImageUri = null }) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    // Modal de Edición de Transacción
    selectedTransactionToEdit?.let { txToEdit ->
        val rawTx = uiState.rawTransactions.find { it.id == txToEdit.id } ?: txToEdit
        EditTransactionModal(
            transaction = rawTx,
            exchangeRate2 = uiState.exchangeRate2,
            onDismiss = { selectedTransactionToEdit = null },
            onSave = { title, amount, category, type, note, timestamp, currency ->
                onEditTransaction(rawTx, title, amount, category, type, note, timestamp, currency)
                selectedTransactionToEdit = null
            },
            onDelete = {
                onDeleteTransaction(rawTx)
                selectedTransactionToEdit = null
            }
        )
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    currencySymbol: String,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryIcon = getCategoryIcon(transaction.category)
    val formattedDate = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("es", "ES"))
        sdf.format(Date(transaction.timestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (transaction.type == "INCOME") IncomeGreen.copy(alpha = 0.15f)
                        else PrimaryEmerald.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category,
                    tint = if (transaction.type == "INCOME") IncomeGreen else PrimaryEmerald,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (transaction.isAiCategorized) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = CategoryPurple.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "IA",
                                    tint = CategoryPurple,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("IA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CategoryPurple)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${transaction.category} • $formattedDate",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount, Edit and Delete buttons
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (transaction.type == "INCOME") "+" else "-"}$currencySymbol${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == "INCOME") IncomeGreen else ExpenseRed
                )
                if (transaction.originalCurrency == "USD" && currencySymbol != "$") {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", transaction.originalAmount)} USD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = SleekPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Alimentación" -> Icons.Default.Fastfood
        "Transporte" -> Icons.Default.DirectionsCar
        "Entretenimiento" -> Icons.Default.Movie
        "Servicios" -> Icons.Default.Receipt
        "Salud" -> Icons.Default.MedicalServices
        "Educación" -> Icons.Default.School
        "Compras" -> Icons.Default.ShoppingBag
        "Sueldo" -> Icons.Default.Payments
        "Inversión" -> Icons.Default.Lightbulb
        else -> Icons.Default.Help
    }
}
