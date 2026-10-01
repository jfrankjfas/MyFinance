package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.auth.GoogleIdentityManager
import com.example.data.model.CurrencyItem
import com.example.data.model.WorldCurrencies
import com.example.ui.FinanceUiState
import com.example.ui.components.AppUpdateCard
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupScreen(
    uiState: FinanceUiState,
    onSetCurrency: (String) -> Unit,
    onSetThreeCurrencies: (CurrencyItem, CurrencyItem, CurrencyItem, Double, Double) -> Unit,
    onSyncGoogleDrive: () -> Unit,
    onRestoreGoogleDrive: () -> Unit,
    onGenerateExcelCsv: () -> Unit,
    onGenerateBackup: () -> Unit,
    onRestoreBackup: (String) -> Unit,
    onClearImportMessage: () -> Unit,
    onLogout: () -> Unit = {},
    onLogin: (String, String) -> Unit = { _, _ -> },
    onSyncFirebase: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var importJsonText by remember { mutableStateOf("") }
    var showBackupMenu by remember { mutableStateOf(false) }
    var showCsvDialog by remember { mutableStateOf(false) }
    var showJsonExportDialog by remember { mutableStateOf(false) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }

    // Currencies customization
    val fav1 = uiState.favoriteCurrencies.getOrElse(0) { WorldCurrencies.DEFAULT_3[0] }
    val fav2 = uiState.favoriteCurrencies.getOrElse(1) { WorldCurrencies.DEFAULT_3[1] }
    val fav3 = uiState.favoriteCurrencies.getOrElse(2) { WorldCurrencies.DEFAULT_3[2] }

    var selectedC1 by remember(fav1) { mutableStateOf(fav1) }
    var selectedC2 by remember(fav2) { mutableStateOf(fav2) }
    var selectedC3 by remember(fav3) { mutableStateOf(fav3) }

    var rate2Input by remember(uiState.exchangeRate2, selectedC2) { mutableStateOf(uiState.exchangeRate2.toString()) }
    var rate3Input by remember(uiState.exchangeRate3, selectedC3) { mutableStateOf(uiState.exchangeRate3.toString()) }

    var showDropdown1 by remember { mutableStateOf(false) }
    var showDropdown2 by remember { mutableStateOf(false) }
    var showDropdown3 by remember { mutableStateOf(false) }


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
                    text = "Opciones y Configuración",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gestiona tu cuenta Gmail, respaldos en Google Drive, Excel y Monedas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Gmail User Account Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SleekPrimaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(SleekPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Cuenta Gmail",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.userName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekOnPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (uiState.isGoogleDriveConnected) IncomeGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (uiState.isGoogleDriveConnected) "Conectado" else "Sin Sesión",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isGoogleDriveConnected) IncomeGreen else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.userEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekOnPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (uiState.isGoogleDriveConnected) {
                            OutlinedButton(
                                onClick = { showSignOutConfirmDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cerrar Sesión", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val am = android.accounts.AccountManager.get(context)
                                        val deviceAccounts = try { am.getAccountsByType("com.google") } catch (e: Exception) { emptyArray() }
                                        if (deviceAccounts.isNotEmpty()) {
                                            val accEmail = deviceAccounts[0].name
                                            val name = accEmail.substringBefore("@")
                                            onLogin(accEmail, name)
                                            Toast.makeText(context, "¡Sesión iniciada con $accEmail!", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }

                                        val googleIdManager = GoogleIdentityManager(context)
                                        googleIdManager.signInWithGoogle(
                                            activityContext = context,
                                            onSuccess = { userData ->
                                                onLogin(userData.email, userData.displayName)
                                                Toast.makeText(context, "¡Sesión iniciada con Google!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { errorMsg ->
                                                if (!errorMsg.contains("cancelado", ignoreCase = true) && !errorMsg.contains("cancellation", ignoreCase = true)) {
                                                    Toast.makeText(context, "⚠️ Error en Google Identity Services: $errorMsg", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                            ) {
                                Text("Continuar con Google", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // App In-App Auto-Update Card
        item {
            AppUpdateCard()
        }

        // Menú Compacto de Respaldos y Sincronización Nube
        item {
            val fbStatus = uiState.firebaseSyncStatus
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, PrimaryEmerald.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PrimaryEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sincronización y Respaldos",
                                tint = PrimaryEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sincronización y Respaldos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (fbStatus.isSyncing) "⏳ Sincronizando con Firebase..."
                                else if (fbStatus.isConnected) "🟢 Firebase Conectado • ${uiState.transactions.size} movs"
                                else fbStatus.syncStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (fbStatus.isConnected) PrimaryEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSyncFirebase,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            enabled = !fbStatus.isSyncing
                        ) {
                            if (fbStatus.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sincronizando...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sincronizar Nube", fontSize = 12.sp)
                            }
                        }

                        Box {
                            OutlinedButton(
                                onClick = { showBackupMenu = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Opciones ▼", fontSize = 12.sp)
                            }

                            DropdownMenu(
                                expanded = showBackupMenu,
                                onDismissRequest = { showBackupMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("☁️ Guardar en Google Drive") },
                                    onClick = {
                                        showBackupMenu = false
                                        onSyncGoogleDrive()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📥 Restaurar de Google Drive") },
                                    onClick = {
                                        showBackupMenu = false
                                        onRestoreGoogleDrive()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📊 Exportar a Excel (CSV)") },
                                    onClick = {
                                        showBackupMenu = false
                                        onGenerateExcelCsv()
                                        showCsvDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("💾 Exportar Respaldo JSON") },
                                    onClick = {
                                        showBackupMenu = false
                                        onGenerateBackup()
                                        showJsonExportDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📄 Importar Respaldo JSON") },
                                    onClick = {
                                        showBackupMenu = false
                                        showJsonImportDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Configuración de 3 Monedas Parámetro
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SleekPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Monedas",
                                tint = SleekPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Configuración Multimoneda (Base: Córdobas C$)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "La moneda principal siempre es Córdobas (C$). Las secundarias se calculan como equivalencias.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SleekPrimaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Regla de Moneda Principal: La base siempre es Córdobas (C$). Los montos se guardan en Córdobas para evitar que los decimales del tipo de cambio alteren tus cifras. El primer cálculo de Córdobas es el que manda.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = SleekOnPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selector Moneda 1 (Principal Fija)
                    CurrencyDropdownSelector(
                        label = "Moneda 1 (Moneda Principal / Base Inmutable)",
                        selectedCurrency = selectedC1,
                        allCurrencies = WorldCurrencies.ALL,
                        expanded = showDropdown1,
                        onExpandChange = { showDropdown1 = it },
                        onSelect = {
                            selectedC1 = it
                            showDropdown1 = false
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Selector Moneda 2 + Rate
                    CurrencyDropdownSelector(
                        label = "Moneda 2 (Secundaria, ej. USD $ Dólar)",
                        selectedCurrency = selectedC2,
                        allCurrencies = WorldCurrencies.ALL,
                        expanded = showDropdown2,
                        onExpandChange = { showDropdown2 = it },
                        onSelect = {
                            selectedC2 = it
                            showDropdown2 = false
                            rate2Input = it.defaultRateToNio.toString()
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = rate2Input,
                        onValueChange = { rate2Input = it },
                        label = { Text("Tasa Oficial: 1 ${selectedC2.code} = X C$ Córdobas") },
                        placeholder = { Text("36.6243") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Selector Moneda 3 + Rate
                    CurrencyDropdownSelector(
                        label = "Moneda 3 (Terciaria, ej. EUR € Euro)",
                        selectedCurrency = selectedC3,
                        allCurrencies = WorldCurrencies.ALL,
                        expanded = showDropdown3,
                        onExpandChange = { showDropdown3 = it },
                        onSelect = {
                            selectedC3 = it
                            showDropdown3 = false
                            rate3Input = it.defaultRateToNio.toString()
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = rate3Input,
                        onValueChange = { rate3Input = it },
                        label = { Text("Tasa Oficial: 1 ${selectedC3.code} = X C$ Córdobas") },
                        placeholder = { Text("39.81") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val r2 = rate2Input.toDoubleOrNull() ?: selectedC2.defaultRateToNio
                            val r3 = rate3Input.toDoubleOrNull() ?: selectedC3.defaultRateToNio
                            onSetThreeCurrencies(selectedC1, selectedC2, selectedC3, r2, r3)
                            Toast.makeText(context, "Configuración guardada: Córdobas como moneda principal", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Configuración de Monedas")
                    }


                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Moneda Activa para Visualización:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(selectedC1, selectedC2, selectedC3).distinctBy { it.symbol }.forEach { item ->
                            val isCurrent = uiState.currencySymbol == item.symbol
                            Surface(
                                color = if (isCurrent) SleekPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable { onSetCurrency(item.symbol) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.flag.isNotEmpty()) {
                                        Text(item.flag, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = "${item.code} (${item.symbol})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    // Modal Dialog: Exportar CSV
    if (showCsvDialog && uiState.csvExportData != null) {
        val csv = uiState.csvExportData!!
        AlertDialog(
            onDismissRequest = { showCsvDialog = false },
            title = { Text("📊 Reporte Excel (.csv)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Reporte exportado en formato CSV listo para Excel y Google Sheets:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = csv,
                            fontSize = 11.sp,
                            maxLines = 10,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("FinanzasClara CSV", csv)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Reporte CSV copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        showCsvDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Modal Dialog: Exportar JSON
    if (showJsonExportDialog && uiState.backupJson != null) {
        val json = uiState.backupJson!!
        AlertDialog(
            onDismissRequest = { showJsonExportDialog = false },
            title = { Text("💾 Respaldo JSON Generado", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Copia este texto JSON para guardarlo o transferirlo a otro dispositivo:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = json,
                            fontSize = 10.sp,
                            maxLines = 10,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("FinanzasClara Backup JSON", json)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Respaldo JSON copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        showJsonExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonExportDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Modal Dialog: Importar JSON
    if (showJsonImportDialog) {
        AlertDialog(
            onDismissRequest = { showJsonImportDialog = false },
            title = { Text("📄 Importar Respaldo JSON", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Pega aquí el texto JSON de respaldo para restaurar tus registros:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("Pega el JSON aquí...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackup(importJsonText)
                        showJsonImportDialog = false
                        importJsonText = ""
                        Toast.makeText(context, "Restaurando datos JSON...", Toast.LENGTH_SHORT).show()
                    },
                    enabled = importJsonText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonImportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            title = {
                Text(
                    text = "Cerrar Sesión de Google",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Deseas cerrar la sesión de '${uiState.userEmail}'?\n\n🔒 Por seguridad y privacidad, todos los registros de movimientos, presupuestos y periodos se quitarán de la aplicación.\n\n☁️ Tus datos están seguros en la nube vinculados a tu cuenta Gmail: ${uiState.userEmail}, y se descargarán automáticamente en cuanto vuelvas a iniciar sesión."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLogout()
                        showSignOutConfirmDialog = false
                        Toast.makeText(context, "Sesión cerrada. Registros retirados de la app.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cerrar Sesión y Quitar Datos")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CurrencyDropdownSelector(
    label: String,
    selectedCurrency: CurrencyItem,
    allCurrencies: List<CurrencyItem>,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelect: (CurrencyItem) -> Unit
) {
    Column {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(true) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedCurrency.flag}  ${selectedCurrency.code} (${selectedCurrency.symbol}) - ${selectedCurrency.country}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(Icons.Default.Edit, contentDescription = "Seleccionar", modifier = Modifier.size(16.dp))
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandChange(false) },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                allCurrencies.forEach { curr ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${curr.flag}  ${curr.code} (${curr.symbol}) - ${curr.country}",
                                fontSize = 13.sp
                            )
                        },
                        onClick = { onSelect(curr) }
                    )
                }
            }
        }
    }
}
