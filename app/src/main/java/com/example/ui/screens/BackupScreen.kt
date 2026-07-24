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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyItem
import com.example.data.model.WorldCurrencies
import com.example.ui.FinanceUiState
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryContainer

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var importJsonText by remember { mutableStateOf("") }
    var showImportField by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

    var loginEmailInput by remember { mutableStateOf("") }
    var loginNameInput by remember { mutableStateOf("") }

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
                                onClick = onLogout,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cerrar Sesión", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Button(
                                onClick = { showLoginDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                            ) {
                                Text("Conectar Cuenta Gmail", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Google Drive Backup Card
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
                                .background(IncomeGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Google Drive",
                                tint = IncomeGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Respaldos en Google Drive",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.lastDriveSync ?: "Vinculación activa con Google Drive",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onSyncGoogleDrive,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar en Drive", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onRestoreGoogleDrive,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar Drive", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Export to Excel (CSV) Card
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
                                imageVector = Icons.Default.TableChart,
                                contentDescription = "Excel CSV",
                                tint = SleekPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exportar a Excel / Hojas de Cálculo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Genera un reporte en formato CSV compatible con Excel y Google Sheets.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onGenerateExcelCsv,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generar Reporte Excel (.csv)")
                    }

                    AnimatedVisibility(visible = uiState.csvExportData != null) {
                        uiState.csvExportData?.let { csv ->
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "📊 Previsualización de Datos Excel:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = SleekPrimary
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("FinanzasClara Excel CSV", csv)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Reporte CSV copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Copiar CSV", fontSize = 11.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = csv,
                                            fontSize = 10.sp,
                                            maxLines = 8,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
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
                                text = "Configuración de Monedas (3 Parámetros)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Elige 3 monedas favoritas de cualquier país para alternar rápidamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selector Moneda 1
                    CurrencyDropdownSelector(
                        label = "Moneda 1 (Parámetro Principal / Base)",
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
                        label = "Moneda 2 (Secundaria, ej. NIO C$ Córdobas)",
                        selectedCurrency = selectedC2,
                        allCurrencies = WorldCurrencies.ALL,
                        expanded = showDropdown2,
                        onExpandChange = { showDropdown2 = it },
                        onSelect = {
                            selectedC2 = it
                            showDropdown2 = false
                            rate2Input = it.defaultRateToUsd.toString()
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = rate2Input,
                        onValueChange = { rate2Input = it },
                        label = { Text("Tasa de cambio: 1 ${selectedC1.code} = X ${selectedC2.symbol}") },
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
                            rate3Input = it.defaultRateToUsd.toString()
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = rate3Input,
                        onValueChange = { rate3Input = it },
                        label = { Text("Tasa de cambio: 1 ${selectedC1.code} = X ${selectedC3.symbol}") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val r2 = rate2Input.toDoubleOrNull() ?: selectedC2.defaultRateToUsd
                            val r3 = rate3Input.toDoubleOrNull() ?: selectedC3.defaultRateToUsd
                            onSetThreeCurrencies(selectedC1, selectedC2, selectedC3, r2, r3)
                            Toast.makeText(context, "3 Monedas y Tasas de cambio actualizadas", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar 3 Monedas y Tasas de Cambio")
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

        // 5. JSON Manual Export / Import Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Copia Manual de Respaldo (JSON)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Exporta o importa el código JSON bruto de tus transacciones para transferencias rápidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onGenerateBackup,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar JSON", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showImportField = !showImportField },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar JSON", fontSize = 12.sp)
                        }
                    }

                    AnimatedVisibility(visible = uiState.backupJson != null) {
                        uiState.backupJson?.let { jsonStr ->
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "✅ Respaldo JSON Generado:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SleekPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = jsonStr,
                                        fontSize = 10.sp,
                                        maxLines = 6,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = showImportField) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = importJsonText,
                                onValueChange = { importJsonText = it },
                                label = { Text("Pega el JSON de respaldo aquí") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 6
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onRestoreBackup(importJsonText)
                                    showImportField = false
                                },
                                enabled = importJsonText.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                            ) {
                                Text("Restaurar Datos JSON")
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.importMessage != null) {
                        uiState.importMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = SleekPrimaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = SleekPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = msg, fontSize = 12.sp, color = SleekOnPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text("Conectar Cuenta de Google / Gmail") },
            text = {
                Column {
                    Text("Ingresa los datos para vincular tu cuenta con Finanzas Claras:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = loginNameInput,
                        onValueChange = { loginNameInput = it },
                        label = { Text("Nombre Completo") },
                        placeholder = { Text("Ej: Francisco J.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = loginEmailInput,
                        onValueChange = { loginEmailInput = it },
                        label = { Text("Correo Gmail") },
                        placeholder = { Text("ejemplo@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = if (loginEmailInput.isNotBlank()) loginEmailInput.trim() else "usuario@gmail.com"
                        val name = if (loginNameInput.isNotBlank()) loginNameInput.trim() else "Usuario Gmail"
                        onLogin(email, name)
                        showLoginDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Iniciar Sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) {
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
