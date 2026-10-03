package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.WarningAmber
import com.example.update.AppUpdateManager
import com.example.update.UpdateInfo
import kotlinx.coroutines.launch

@Composable
fun AppUpdateCard(
    currentVersion: String = "2.1",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val updateManager = remember { AppUpdateManager(context) }

    val currentVer = remember {
        val installed = updateManager.getInstalledVersionName()
        if (installed.isNotBlank()) installed else "2.1"
    }

    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isUpToDate by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    var showRepoDialog by remember { mutableStateOf(false) }
    var repoInput by remember { mutableStateOf(updateManager.getRepositoryName()) }

    var showPublishDialog by remember { mutableStateOf(false) }
    var publishVersion by remember { mutableStateOf("2.1") }
    var publishNotes by remember { mutableStateOf("Versión 2.1: Continuación automática de presupuesto entre quincenas al superar el 100%, selección de origen de fondos (Presupuesto o Fondos Extraordinarios) en Gastos Programados, soporte completo para pagos recurrentes mensuales con renovación automática y corrección en la edición/eliminación de registros e inversiones.") }
    var publishApkUrl by remember { mutableStateOf("https://github.com/${updateManager.getRepositoryName()}/releases/download/v2.1/FinanzasClaras-v2.1.apk") }
    var isPublishing by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SleekPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Actualizaciones",
                        tint = SleekPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Actualizaciones Automáticas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Versión: v$currentVer • Repo: ${updateManager.getRepositoryName()}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { 
                    showPublishDialog = !showPublishDialog 
                }) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Publicar Versión en Firebase",
                        tint = SleekPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { 
                    repoInput = updateManager.getRepositoryName()
                    showRepoDialog = !showRepoDialog 
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Configurar Repositorio",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showPublishDialog) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SleekPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🚀 Publicar Versión en la Nube (Firebase)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Al guardar, cualquier dispositivo que tenga la app instalada detectará la actualización inmediatamente al pulsar 'Comprobar Actualizaciones'.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = publishVersion,
                            onValueChange = { publishVersion = it },
                            label = { Text("Versión a publicar (ej: 1.3)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = publishApkUrl,
                            onValueChange = { publishApkUrl = it },
                            label = { Text("URL Directa del APK") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = publishNotes,
                            onValueChange = { publishNotes = it },
                            label = { Text("Notas de la versión") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isPublishing = true
                                coroutineScope.launch {
                                    try {
                                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        val data = hashMapOf(
                                            "latestVersion" to publishVersion.trim(),
                                            "releaseNotes" to publishNotes.trim(),
                                            "apkDownloadUrl" to publishApkUrl.trim(),
                                            "releaseDate" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                                            "updatedAt" to System.currentTimeMillis()
                                        )
                                        db.collection("app_config").document("version")
                                            .set(data, com.google.firebase.firestore.SetOptions.merge())
                                        Toast.makeText(context, "✅ Versión v$publishVersion publicada en Firebase!", Toast.LENGTH_LONG).show()
                                        showPublishDialog = false
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isPublishing = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isPublishing && publishVersion.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publicando...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardar en Firebase (app_config/version)", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showRepoDialog) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Repositorio GitHub para actualizaciones:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = repoInput,
                            onValueChange = { repoInput = it },
                            placeholder = { Text("jfrankjfas/MyFinance") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                updateManager.setRepositoryName(repoInput)
                                showRepoDialog = false
                                Toast.makeText(context, "Repositorio guardado: ${updateManager.getRepositoryName()}", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Text("Guardar", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ℹ️ ¿Cómo se detectan los cambios? La app compara la versión actual (v$currentVer) contra la última Release o Tag en GitHub (jfrankjfas/MyFinance) o Firestore. Cuando publiques una nueva versión (ej: v1.1), se detectará automáticamente al iniciar la app.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Result states
            if (isUpToDate) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IncomeGreen.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "✅ Tu app está en la versión v$currentVer.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen
                            )
                        }
                        updateInfo?.releaseNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notes,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = err,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (err.contains("Releases") || err.contains("GitHub") || err.contains("404")) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val browserIntent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://github.com/${updateManager.getRepositoryName()}/releases")
                                    )
                                    context.startActivity(browserIntent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("🌐 Abrir Releases en GitHub", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            updateInfo?.let { info ->
                if (info.hasUpdate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WarningAmber.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NewReleases,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "¡Nueva versión disponible! v${info.latestVersion}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (info.releaseDate.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Publicado: ${info.releaseDate}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = info.releaseNotes,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (isDownloading) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Descargando actualización...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${(downloadProgress * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = SleekPrimary
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (activity != null) {
                                            isDownloading = true
                                            downloadProgress = 0f
                                            coroutineScope.launch {
                                                val res = updateManager.downloadAndInstallApk(
                                                    activity = activity,
                                                    apkUrl = info.apkDownloadUrl,
                                                    onProgress = { downloadProgress = it }
                                                )
                                                isDownloading = false
                                                if (res.isFailure) {
                                                    errorMessage = res.exceptionOrNull()?.message
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "No se pudo acceder a la actividad", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Descargar e Instalar Actualización")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action button: Buscar actualizaciones
            OutlinedButton(
                onClick = {
                    isChecking = true
                    errorMessage = null
                    isUpToDate = false
                    updateInfo = null
                    coroutineScope.launch {
                        val result = updateManager.checkForUpdates(currentVer)
                        isChecking = false
                        if (result.isSuccess) {
                            val info = result.getOrNull()
                            updateInfo = info
                            if (info != null && !info.hasUpdate) {
                                isUpToDate = true
                            }
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Error al comprobar actualizaciones"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isChecking && !isDownloading
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = SleekPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Comprobando actualizaciones...", fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Buscar actualizaciones",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Comprobar Actualizaciones Ahora", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    isUpToDate = false
                    errorMessage = null
                    updateInfo = UpdateInfo(
                        hasUpdate = true,
                        latestVersion = "1.8",
                        releaseNotes = "🧪 Actualización de prueba (v1.8): Verificación completa de descarga e instalación para Finanzas Claras. Descarga el paquete oficial APK y ejecuta el instalador del sistema.",
                        apkDownloadUrl = "https://github.com/${updateManager.getRepositoryName()}/releases/download/v1.2/FinanzasClara-v1.2.apk",
                        releaseDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NewReleases,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = WarningAmber
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Probar Detección y Descarga de Actualización", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
