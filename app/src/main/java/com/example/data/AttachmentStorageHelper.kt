package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object AttachmentStorageHelper {

    private const val TAG = "AttachmentStorageHelper"

    /**
     * Copies a selected photo or document to internal app storage so that the URI
     * persists indefinitely across reboots and permissions changes.
     */
    fun copyUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val attachmentsDir = File(context.filesDir, "attachments").apply {
                if (!exists()) mkdirs()
            }

            val extension = when (context.contentResolver.getType(sourceUri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/jpeg" -> "jpg"
                else -> "jpg"
            }

            val destFile = File(attachmentsDir, "receipt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension")

            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val outputStream = FileOutputStream(destFile)

            if (inputStream != null) {
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(destFile).toString()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy attachment: ${e.message}", e)
            sourceUri.toString()
        }
    }
}
