package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID = "finanzas_clara_budget_alerts"
    private const val CHANNEL_NAME = "Alertas de Presupuesto"

    private const val PAYMENT_CHANNEL_ID = "finanzas_clara_payment_reminders"
    private const val PAYMENT_CHANNEL_NAME = "Recordatorios de Pago y Vencimientos"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val budgetChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificaciones automáticas cuando tus gastos exceden el presupuesto configurado."
                enableVibration(true)
                enableLights(true)
                setSound(defaultSoundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(budgetChannel)

            val paymentChannel = NotificationChannel(PAYMENT_CHANNEL_ID, PAYMENT_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alertas programadas 2 días antes, 1 día antes y el día del vencimiento de tus pagos."
                enableVibration(true)
                enableLights(true)
                setSound(defaultSoundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(paymentChannel)
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun sendTestNotification(context: Context, currencySymbol: String): Boolean {
        createNotificationChannel(context)
        if (!areNotificationsEnabled(context)) {
            Log.w(TAG, "Notifications are disabled by system permissions")
            return false
        }

        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val title = "🔔 Finanzas Claras: ¡Notificaciones Activas!"
            val message = "Tus alertas de presupuesto y recordatorios de pago están configurados correctamente. Moneda: $currencySymbol"

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val builder = NotificationCompat.Builder(context, PAYMENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setSound(defaultSoundUri)
                .setVibrate(longArrayOf(0, 300, 200, 300))
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(9999, builder.build())
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting notification: ${e.message}", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error posting test notification: ${e.message}", e)
            return false
        }
    }

    fun sendBudgetAlertNotification(
        context: Context,
        category: String,
        spentAmount: Double,
        limitAmount: Double,
        percent: Int
    ) {
        createNotificationChannel(context)
        if (!areNotificationsEnabled(context)) return

        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val title = if (percent >= 100) {
                "🚨 Presupuesto Excedido: $category"
            } else {
                "⚠️ Alerta de Presupuesto: $category"
            }

            val message = if (percent >= 100) {
                "¡Atención! Has gastado ${String.format("%.2f", spentAmount)} de tu límite de ${String.format("%.2f", limitAmount)} ($percent%)."
            } else {
                "Llevas gastado el $percent% de tu presupuesto de $category (${String.format("%.2f", spentAmount)} de ${String.format("%.2f", limitAmount)})."
            }

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(defaultSoundUri)
                .setAutoCancel(true)

            val notificationId = (category.hashCode() + percent)
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error posting budget alert: ${e.message}", e)
        }
    }

    fun sendScheduledExpenseReminder(
        context: Context,
        title: String,
        amountFormatted: String,
        dueDateText: String
    ) {
        createNotificationChannel(context)
        if (!areNotificationsEnabled(context)) return

        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notifTitle = "⏰ Recordatorio de Pago Pendiente: $title"
            val message = "Recuerda que tienes programado un pago de $amountFormatted por '$title' que vence $dueDateText."

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val builder = NotificationCompat.Builder(context, PAYMENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(notifTitle)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(defaultSoundUri)
                .setAutoCancel(true)

            val notificationId = (title.hashCode() + (System.currentTimeMillis() % 10000).toInt())
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error posting scheduled expense reminder: ${e.message}", e)
        }
    }

    fun sendPaymentDueReminder(
        context: Context,
        title: String,
        amountFormatted: String,
        daysRemaining: Int, // 2 for two days before, 1 for one day before, 0 for today
        dueDateFormatted: String
    ) {
        createNotificationChannel(context)
        if (!areNotificationsEnabled(context)) return

        try {
            val (notifTitle, message) = when (daysRemaining) {
                2 -> Pair(
                    "⏳ En 2 días vence tu pago: $title",
                    "Faltan 2 días para tu pago de $amountFormatted ($dueDateFormatted). Aparta tus fondos para evitar intereses moratorios."
                )
                1 -> Pair(
                    "🚨 ¡MAÑANA VENCE!: $title",
                    "¡Atención urgente! Mañana vence el pago de $amountFormatted por '$title'. Realiza tu pago a tiempo para proteger tus finanzas."
                )
                else -> Pair(
                    "📅 ¡HOY ES EL DÍA DE PAGO!: $title",
                    "Hoy es la fecha límite para pagar $amountFormatted de '$title'. Evita recargos y cortes de servicio."
                )
            }

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val builder = NotificationCompat.Builder(context, PAYMENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(notifTitle)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setSound(defaultSoundUri)
                .setVibrate(longArrayOf(0, 400, 200, 400))
                .setAutoCancel(true)

            val notificationId = ("payment_${title}_$daysRemaining".hashCode())
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error posting payment due reminder: ${e.message}", e)
        }
    }
}
