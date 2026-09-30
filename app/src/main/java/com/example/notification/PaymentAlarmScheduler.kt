package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.entity.ScheduledExpenseEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PaymentAlarmScheduler {

    private const val TAG = "PaymentAlarmScheduler"

    fun schedulePaymentReminders(context: Context, expense: ScheduledExpenseEntity, currencySymbol: String) {
        if (!expense.notifyReminder || expense.isPaid) {
            cancelPaymentReminders(context, expense.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dueDateFormatted = sdf.format(Date(expense.dueDate))
        val amountFormatted = "$currencySymbol${String.format(Locale.US, "%.2f", expense.amount)}"

        // We schedule alarms for:
        // 1. Exactly 2 days before due date (at 09:00 AM)
        // 2. Exactly 1 day before due date (at 09:00 AM)
        // 3. Due date morning (at 08:30 AM)
        val targets = listOf(
            Pair(2, calculateReminderTime(expense.dueDate, daysBefore = 2, hour = 9, minute = 0)),
            Pair(1, calculateReminderTime(expense.dueDate, daysBefore = 1, hour = 9, minute = 0)),
            Pair(0, calculateReminderTime(expense.dueDate, daysBefore = 0, hour = 8, minute = 30))
        )

        val now = System.currentTimeMillis()
        var scheduledAny = false

        targets.forEach { (daysBefore, triggerMs) ->
            if (triggerMs > now) {
                scheduleSingleAlarm(context, alarmManager, expense, daysBefore, triggerMs, amountFormatted, dueDateFormatted)
                scheduledAny = true
            }
        }

        // Si todos los horarios calculados quedaron en el pasado pero el gasto vence hoy o está por vencer
        if (!scheduledAny && expense.dueDate >= now - (24 * 3600 * 1000L)) {
            val immediateTrigger = now + 4000L // Alerta en 4 segundos
            scheduleSingleAlarm(context, alarmManager, expense, 0, immediateTrigger, amountFormatted, dueDateFormatted)
            Log.d(TAG, "Scheduled immediate alarm for upcoming/today payment '${expense.title}'")
        }
    }

    private fun scheduleSingleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        expense: ScheduledExpenseEntity,
        daysBefore: Int,
        triggerMs: Long,
        amountFormatted: String,
        dueDateFormatted: String
    ) {
        val intent = Intent(context, PaymentAlarmReceiver::class.java).apply {
            putExtra(PaymentAlarmReceiver.EXTRA_TITLE, expense.title)
            putExtra(PaymentAlarmReceiver.EXTRA_AMOUNT, amountFormatted)
            putExtra(PaymentAlarmReceiver.EXTRA_DAYS_REMAINING, daysBefore)
            putExtra(PaymentAlarmReceiver.EXTRA_DUE_DATE_FORMATTED, dueDateFormatted)
        }

        val requestCode = (expense.id * 10 + daysBefore).toInt()
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            }
            Log.d(TAG, "Scheduled alarm for '${expense.title}' in $daysBefore days at $triggerMs")
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot schedule exact alarm: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
        }
    }

    fun cancelPaymentReminders(context: Context, expenseId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        listOf(2, 1, 0).forEach { daysBefore ->
            val intent = Intent(context, PaymentAlarmReceiver::class.java)
            val requestCode = (expenseId * 10 + daysBefore).toInt()
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun calculateReminderTime(dueTimestamp: Long, daysBefore: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dueTimestamp
            add(Calendar.DAY_OF_YEAR, -daysBefore)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
