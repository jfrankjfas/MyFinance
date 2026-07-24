package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

object NotificationHelper {

    private const val CHANNEL_ID = "finanzas_clara_budget_alerts"
    private const val CHANNEL_NAME = "Alertas de Presupuesto"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notificaciones automáticas cuando tus gastos exceden el presupuesto configurado."
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = if (percent >= 100) {
            "🚨 Presupuesto Excedido: $category"
        } else {
            "⚠️ Alerta de Presupuesto: $category"
        }

        val message = if (percent >= 100) {
            "¡Atención! Has gastado $${String.format("%.2f", spentAmount)} de tu límite de $${String.format("%.2f", limitAmount)} ($percent%)."
        } else {
            "Llevas gastado el $percent% de tu presupuesto de $category ($${String.format("%.2f", spentAmount)} de $${String.format("%.2f", limitAmount)})."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationId = (category.hashCode() + percent)
        notificationManager.notify(notificationId, builder.build())
    }

    fun sendScheduledExpenseReminder(
        context: Context,
        title: String,
        amountFormatted: String,
        dueDateText: String
    ) {
        createNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notifTitle = "⏰ Recordatorio de Pago Pendiente: $title"
        val message = "Recuerda que tienes programado un pago de $amountFormatted por '$title' que vence $dueDateText."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notifTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationId = (title.hashCode() + System.currentTimeMillis().toInt() % 10000)
        notificationManager.notify(notificationId, builder.build())
    }
}
