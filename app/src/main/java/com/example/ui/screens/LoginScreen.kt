package com.example.ui.screens

import android.accounts.AccountManager
import android.app.Activity
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.GoogleIdentityManager
import com.example.security.AppSecurityManager
import com.example.ui.FinanceUiState
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryContainer
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    uiState: FinanceUiState,
    securityManager: AppSecurityManager?,
    onLogin: (email: String, name: String, pin: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val googleIdManager = remember { GoogleIdentityManager(context) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var showSecretManualLogin by remember { mutableStateOf(false) }

    // Detect Google accounts already logged in on this Android device/emulator
    val deviceGoogleAccounts = remember {
        try {
            val am = AccountManager.get(context)
            am.getAccountsByType("com.google").map { it.name }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // System Android Account Picker Launcher
    val accountChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isGoogleSigningIn = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val chosenEmail = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!chosenEmail.isNullOrBlank()) {
                val cleanEmail = chosenEmail.trim().lowercase(Locale.ROOT)
                val displayName = cleanEmail.substringBefore("@").replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                Toast.makeText(context, "✅ Sesión con Google: $cleanEmail", Toast.LENGTH_SHORT).show()
                onLogin(cleanEmail, displayName, null)
            }
        }
    }

    fun launchGoogleSignIn() {
        isGoogleSigningIn = true
        errorMessage = null

        // 1. If device already has exactly 1 Google account, log in with it directly
        if (deviceGoogleAccounts.size == 1) {
            val email = deviceGoogleAccounts[0]
            isGoogleSigningIn = false
            val name = email.substringBefore("@").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            Toast.makeText(context, "✅ Sesión iniciada con Google: $email", Toast.LENGTH_SHORT).show()
            onLogin(email, name, null)
            return
        }

        // 2. Otherwise try Google Identity Services CredentialManager
        coroutineScope.launch {
            googleIdManager.signInWithGoogle(
                activityContext = context,
                onSuccess = { userData ->
                    isGoogleSigningIn = false
                    Toast.makeText(context, "✅ Identidad verificada con Google: ${userData.email}", Toast.LENGTH_SHORT).show()
                    onLogin(userData.email, userData.displayName, null)
                },
                onError = { _ ->
                    // 3. Seamless fallback to native Android System Account Chooser intent
                    try {
                        val chooseIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
                        } else {
                            @Suppress("DEPRECATION")
                            AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), false, null, null, null, null)
                        }
                        accountChooserLauncher.launch(chooseIntent)
                    } catch (e: Exception) {
                        isGoogleSigningIn = false
                        Log.w("LoginScreen", "Account chooser failed: ${e.message}")
                        if (deviceGoogleAccounts.isNotEmpty()) {
                            val email = deviceGoogleAccounts[0]
                            val name = email.substringBefore("@")
                            onLogin(email, name, null)
                        } else {
                            errorMessage = "No se pudo seleccionar cuenta Google en el dispositivo. Toca la letra final del título para acceso directo."
                        }
                    }
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon & Logo
            Surface(
                shape = CircleShape,
                color = SleekPrimaryContainer,
                modifier = Modifier.size(100.dp),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Logo Finanzas Claras",
                        tint = SleekPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Title with secret backdoor in the last letter 's'
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Finanzas Clara",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Touching the final 's' reveals the secret direct email input dialog
                Text(
                    text = "s",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clickable {
                            showSecretManualLogin = true
                        }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Control Inteligente de Finanzas y Presupuestos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Trust & Features Badges
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = PrimaryEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Firebase Cloud Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = SleekPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PIN & Biometría", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Official Google Authentication Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Acceso Seguro con Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Para proteger tu privacidad, el acceso se realiza mediante tu cuenta oficial de Google.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Google Sign-In Button
                    Button(
                        onClick = { launchGoogleSignIn() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekPrimary
                        ),
                        enabled = !isGoogleSigningIn && !uiState.isAiLoading
                    ) {
                        if (isGoogleSigningIn || uiState.isAiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Verificando cuenta Google...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Google",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Iniciar Sesión con Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // If Google accounts were detected on device, show direct 1-tap option
                    if (deviceGoogleAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Cuenta Google detectada en tu dispositivo:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        deviceGoogleAccounts.forEach { accEmail ->
                            OutlinedButton(
                                onClick = {
                                    val name = accEmail.substringBefore("@").replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                    }
                                    Toast.makeText(context, "✅ Sesión iniciada: $accEmail", Toast.LENGTH_SHORT).show()
                                    onLogin(accEmail, name, null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Entrar como $accEmail", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    AnimatedVisibility(visible = errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = ExpenseRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ ${errorMessage ?: ""}",
                                color = ExpenseRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Status message during sync
            if (!uiState.importMessage.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.importMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Close App Button if user wants to exit
            OutlinedButton(
                onClick = {
                    (context as? Activity)?.finishAffinity()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Salir de la Aplicación", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Secret manual login dialog (activated by touching the letter 'a' in "Finanzas Clara")
    if (showSecretManualLogin) {
        var secretEmail by remember { mutableStateOf("") }
        var secretPin by remember { mutableStateOf("") }
        var secretError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSecretManualLogin = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = SleekPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Acceso por Correo",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Digita tu dirección de correo para acceder directamente:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = secretEmail,
                        onValueChange = {
                            secretEmail = it
                            secretError = null
                        },
                        label = { Text("Correo Electrónico") },
                        placeholder = { Text("tu_cuenta@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = secretPin,
                        onValueChange = {
                            if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                                secretPin = it
                            }
                        },
                        label = { Text("PIN de Seguridad (Opcional)") },
                        placeholder = { Text("4 dígitos numéricos") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    secretError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ $err",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = secretEmail.trim().lowercase(Locale.ROOT)
                        if (clean.isBlank() || !clean.contains("@")) {
                            secretError = "Por favor ingresa un correo válido."
                            return@Button
                        }
                        showSecretManualLogin = false
                        val name = clean.substringBefore("@").replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        }
                        onLogin(clean, name, if (secretPin.isNotBlank()) secretPin else null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Ingresar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecretManualLogin = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
