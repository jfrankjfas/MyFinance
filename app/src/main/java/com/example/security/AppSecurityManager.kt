package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class AppSecurityManager(private val context: Context) {

    companion object {
        private const val TAG = "AppSecurityManager"
        private const val PREFS_NAME = "finanzas_clara_security"
        private const val KEY_SECURITY_ENABLED = "security_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val DEFAULT_PIN = "1234"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isLocked = MutableStateFlow(isSecurityEnabled())
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isSecurityEnabledState = MutableStateFlow(isSecurityEnabled())
    val isSecurityEnabledState: StateFlow<Boolean> = _isSecurityEnabledState.asStateFlow()

    private val _isBiometricEnabledState = MutableStateFlow(isBiometricEnabled())
    val isBiometricEnabledState: StateFlow<Boolean> = _isBiometricEnabledState.asStateFlow()

    init {
        // If security was never initialized, initialize with default active security and PIN "1234"
        if (!prefs.contains(KEY_SECURITY_ENABLED)) {
            val defaultHash = hashPin(DEFAULT_PIN)
            prefs.edit()
                .putBoolean(KEY_SECURITY_ENABLED, true)
                .putString(KEY_PIN_HASH, defaultHash)
                .putBoolean(KEY_BIOMETRIC_ENABLED, true)
                .apply()
            _isSecurityEnabledState.value = true
            _isBiometricEnabledState.value = true
            _isLocked.value = true
        }
    }

    fun isSecurityEnabled(): Boolean {
        return prefs.getBoolean(KEY_SECURITY_ENABLED, true)
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setSecurityEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURITY_ENABLED, enabled).apply()
        _isSecurityEnabledState.value = enabled
        if (!enabled) {
            _isLocked.value = false
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabledState.value = enabled
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, hashPin(DEFAULT_PIN))
        val inputHash = hashPin(pin)
        val valid = storedHash == inputHash
        if (valid) {
            _isLocked.value = false
        }
        return valid
    }

    fun getPinLength(): Int {
        return prefs.getInt("pin_length", 4)
    }

    fun getRawPin(): String {
        return prefs.getString("saved_pin_raw", DEFAULT_PIN) ?: DEFAULT_PIN
    }

    fun updatePin(newPin: String): Boolean {
        if (newPin.length < 4 || newPin.length > 8) return false
        val newHash = hashPin(newPin)
        prefs.edit()
            .putString(KEY_PIN_HASH, newHash)
            .putInt("pin_length", newPin.length)
            .putString("saved_pin_raw", newPin)
            .apply()
        return true
    }

    fun setPinFromCloud(cloudPin: String) {
        if (cloudPin.isNotBlank() && cloudPin.length in 4..8) {
            val newHash = hashPin(cloudPin)
            prefs.edit()
                .putString(KEY_PIN_HASH, newHash)
                .putInt("pin_length", cloudPin.length)
                .putString("saved_pin_raw", cloudPin)
                .apply()
            Log.d(TAG, "PIN updated from Firebase Firestore (pinuser)")
        }
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun lock() {
        if (isSecurityEnabled()) {
            _isLocked.value = true
        }
    }

    fun canAuthenticateWithBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Acceso a Finanzas Clara",
        subtitle: String = "Usa tu huella dactilar o reconocimiento biométrico",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!canAuthenticateWithBiometrics()) {
            onError("Biometría no disponible en este dispositivo. Usa tu PIN.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Usar PIN de Seguridad")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlock()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.d(TAG, "Biometric error: $errorCode - $errString")
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Huella no reconocida. Intenta de nuevo o ingresa tu PIN.")
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching biometric prompt: ${e.message}", e)
            onError(e.message ?: "Error al iniciar biometría")
        }
    }

    private fun hashPin(pin: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            pin
        }
    }
}
