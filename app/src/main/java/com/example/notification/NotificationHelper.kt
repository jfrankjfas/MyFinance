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

    private const val PAYMENT_CHANNEL_ID = "finanzas_clara_payment_reminders"
    private const val PAYMENT_CHANNEL_NAME = "Recordatorios de Pago y Vencimientos"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val budgetChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notificaciones automáticas cuando tus gastos exceden el presupuesto configurado."
            }
            notificationManager.createNotificationChannel(budgetChannel)

            val paymentChannel = NotificationChannel(PAYMENT_CHANNEL_ID, PAYMENT_CHANNEL_NAME, importance).apply {
                description = "Alertas programadas 2 días antes, 1 día antes y el día del vencimiento de tus pagos."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(paymentChannel)
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

    fun sendPaymentDueReminder(
        context: Context,
        title: String,
        amountFormatted: String,
        daysRemaining: Int, // 2 for two days before, 1 for one day before, 0 for today
        dueDateFormatted: String
    ) {
        createNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val builder = NotificationCompat.Builder(context, PAYMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notifTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)

        val notificationId = ("payment_${title}_$daysRemaining".hashCode())
        notificationManager.notify(notificationId, builder.build())
    }
}
