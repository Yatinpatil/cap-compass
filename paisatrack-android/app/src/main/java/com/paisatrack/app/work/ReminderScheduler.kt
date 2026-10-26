package com.paisatrack.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.await
import com.paisatrack.app.R
import com.paisatrack.app.data.local.PaisaTrackDatabase
import com.paisatrack.app.data.local.ReminderEntity
import com.paisatrack.app.data.local.SensitiveLong
import com.paisatrack.app.data.privacy.KeystoreCipher
import com.paisatrack.app.domain.model.DueDateSource
import com.paisatrack.app.domain.model.ReminderStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/** WorkManager reminders are intentionally inexact and never infer a due date. */
class ReminderScheduler(private val context: Context, private val database: PaisaTrackDatabase) {
    suspend fun schedule(paymentId: String, dueDateEpochMillis: Long?, daysBefore: Int, confirmed: Boolean, append: Boolean = false) {
        val workName = workName(paymentId)
        if (!confirmed || dueDateEpochMillis == null) {
            if (!append) WorkManager.getInstance(context).cancelUniqueWork(workName).await()
            return
        }
        if (!append) WorkManager.getInstance(context).cancelUniqueWork(workName).await()
        val triggerAt = Instant.ofEpochMilli(dueDateEpochMillis).minus(daysBefore.toLong(), ChronoUnit.DAYS).toEpochMilli()
        val delay = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<BillReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(BillReminderWorker.PAYMENT_ID to paymentId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            if (append) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
            request,
        ).await()
        database.reminders().upsert(
            ReminderEntity(
                id = paymentId,
                recurringPaymentId = paymentId,
                scheduledAt = SensitiveLong(triggerAt),
                status = ReminderStatus.SCHEDULED,
                workName = workName,
            ),
        )
    }

    suspend fun cancel(paymentId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(paymentId)).await()
        database.reminders().deleteForPayment(paymentId)
    }

    suspend fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWork().await()
    }

    private fun workName(paymentId: String) = "bill-reminder:$paymentId"
}

class BillReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val paymentId = inputData.getString(PAYMENT_ID) ?: return Result.failure()
        val app = applicationContext as? com.paisatrack.app.PaisaTrackApplication ?: return Result.failure()
        val db = app.container.database
        val payment = db.recurringPayments().findById(paymentId) ?: return Result.success()
        if (!payment.enabled || payment.dueDateSource == DueDateSource.INFERRED_UNCONFIRMED) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            db.reminders().observeForPayment(paymentId).first()
                .firstOrNull()?.let { reminder -> db.reminders().upsert(reminder.copy(status = ReminderStatus.PAUSED_PERMISSION)) }
            return Result.success()
        }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Bill reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_paisatrack)
            .setContentTitle("Payment reminder")
            .setContentText("A bill or renewal you saved is coming up.")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(applicationContext).notify(paymentId.hashCode(), notification) }
        db.reminders().deleteForPayment(paymentId)
        if (payment.frequency != com.paisatrack.app.domain.model.BillingFrequency.ONE_TIME) {
            val currentDue = payment.dueDate?.value ?: return Result.success()
            val zone = ZoneId.systemDefault()
            var nextDue = Instant.ofEpochMilli(currentDue).atZone(zone).toLocalDate()
            val today = LocalDate.now(zone)
            var advances = 0
            do {
                nextDue = when (payment.frequency) {
                    com.paisatrack.app.domain.model.BillingFrequency.WEEKLY -> nextDue.plusWeeks(1)
                    com.paisatrack.app.domain.model.BillingFrequency.MONTHLY -> nextDue.plusMonths(1)
                    com.paisatrack.app.domain.model.BillingFrequency.QUARTERLY -> nextDue.plusMonths(3)
                    com.paisatrack.app.domain.model.BillingFrequency.YEARLY -> nextDue.plusYears(1)
                    com.paisatrack.app.domain.model.BillingFrequency.ONE_TIME -> nextDue
                }
                advances++
            } while (!nextDue.isAfter(today) && advances < 120)
            val nextDueMillis = nextDue.atStartOfDay(zone).toInstant().toEpochMilli()
            val updated = payment.copy(dueDate = SensitiveLong(nextDueMillis), enabled = true)
            db.recurringPayments().upsert(updated)
            app.container.reminderScheduler.schedule(paymentId, nextDueMillis, payment.daysBeforeReminder, true, append = true)
        } else {
            db.recurringPayments().upsert(payment.copy(enabled = false))
        }
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "bill_reminders"
        const val PAYMENT_ID = "payment_id"
    }
}
