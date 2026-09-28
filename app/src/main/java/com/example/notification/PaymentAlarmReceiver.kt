package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PaymentAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Gasto Programado"
        val amountFormatted = intent.getStringExtra(EXTRA_AMOUNT) ?: "$0.00"
        val daysRemaining = intent.getIntExtra(EXTRA_DAYS_REMAINING, 1)
        val dueDateFormatted = intent.getStringExtra(EXTRA_DUE_DATE_FORMATTED) ?: ""

        Log.d("PaymentAlarmReceiver", "Received payment alarm for '$title' ($daysRemaining days remaining)")

        NotificationHelper.sendPaymentDueReminder(
            context = context,
            title = title,
            amountFormatted = amountFormatted,
            daysRemaining = daysRemaining,
            dueDateFormatted = dueDateFormatted
        )
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_DAYS_REMAINING = "extra_days_remaining"
        const val EXTRA_DUE_DATE_FORMATTED = "extra_due_date_formatted"
    }
}
