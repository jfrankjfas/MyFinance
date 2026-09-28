package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

data class GoogleUserData(
    val email: String,
    val displayName: String,
    val idToken: String,
    val profilePictureUri: String? = null
)

class GoogleIdentityManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleIdentityManager"
        // Standard Web Client ID for OAuth 2.0 and OpenID Connect with Google Identity Services
        // This is paired with Firebase App / Google Cloud OAuth 2.0 Client
        private const val WEB_CLIENT_ID = "897107668731-017e8n3f2834s2i3j8q0jsh7u6l92r9f.apps.googleusercontent.com"
    }

    private val credentialManager: CredentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(
        activityContext: Context,
        onSuccess: (GoogleUserData) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            try {
                // Generate a cryptographically secure nonce for OpenID Connect protocol
                val rawNonce = UUID.randomUUID().toString()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(rawNonce.toByteArray())
                val hashedNonce = digest.joinToString("") { "%02x".format(it) }

                // Build Google Identity Services OAuth 2.0 / OpenID Connect Option
                // filterByAuthorizedAccounts = false allows ANY Google/Gmail account to be selected
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )

                val credential = response.credential
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val userData = GoogleUserData(
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id.substringBefore("@"),
                        idToken = googleIdTokenCredential.idToken,
                        profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                    Log.d(TAG, "Google Identity Sign-In Success: ${userData.email}")
                    onSuccess(userData)
                } else {
                    onError("Tipo de credencial no reconocido: ${credential.type}")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "User cancelled Google Sign-In")
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Google Identity Services error: ${e.message}")
                // If Play Services is missing or client ID needs registration, provide informative message
                onError("Google Identity Services: ${e.message ?: "Inicio cancelado o no disponible"}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in Google Sign-In: ${e.message}", e)
                onError("Error inesperado en Google Sign-In: ${e.message}")
            }
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.Main) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                Log.d(TAG, "Google Identity credential state cleared.")
            } catch (e: Exception) {
                Log.w(TAG, "Error clearing credential state: ${e.message}")
            }
        }
    }
}
