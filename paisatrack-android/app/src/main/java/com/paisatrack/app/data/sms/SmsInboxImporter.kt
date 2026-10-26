package com.paisatrack.app.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import com.paisatrack.app.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsImportOutcome(
    val scanned: Int = 0,
    val queuedForReview: Int = 0,
    val duplicates: Int = 0,
    val skipped: Int = 0,
    val permissionDenied: Boolean = false,
)

class SmsInboxImporter(
    private val context: Context,
    private val repository: FinanceRepository,
    private val ingestion: SmsIngestionService,
) {
    /** Imports recent inbox pages on IO. SMS bodies remain transient parser input only. */
    suspend fun importExistingMessages(): SmsImportOutcome = withContext(Dispatchers.IO) {
        if (context.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext SmsImportOutcome(permissionDenied = true)
        }
        if (!repository.smsImportEnabled()) return@withContext SmsImportOutcome()

        var checkpoint = repository.inboxCheckpoint()
        val initialCutoff = System.currentTimeMillis() - IMPORT_LOOKBACK_MILLIS
        var scanned = 0
        var queued = 0
        var duplicates = 0
        var skipped = 0

        try {
            while (true) {
                val checkpointDate = checkpoint?.lastMessageDate?.value ?: initialCutoff
                val checkpointId = checkpoint?.lastProviderId?.value ?: -1L
                val selection = "(${Telephony.Sms.DATE} > ? OR (${Telephony.Sms.DATE} = ? AND ${Telephony.Sms._ID} > ?))"
                val selectionArgs = arrayOf(checkpointDate.toString(), checkpointDate.toString(), checkpointId.toString())
                var pageRows = 0
                context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                    selection,
                    selectionArgs,
                    "${Telephony.Sms.DATE} ASC, ${Telephony.Sms._ID} ASC",
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val addressColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    while (pageRows < PAGE_SIZE && cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val date = cursor.getLong(dateColumn)
                        val envelope = SmsEnvelope(
                            senderAddress = cursor.getString(addressColumn).orEmpty(),
                            body = cursor.getString(bodyColumn).orEmpty(),
                            receivedAtEpochMillis = date,
                            providerMessageId = id,
                        )
                        when (ingestion.process(envelope)) {
                            SmsIngestionResult.QUEUED_FOR_REVIEW -> queued++
                            SmsIngestionResult.DUPLICATE -> duplicates++
                            SmsIngestionResult.SKIPPED_INELIGIBLE -> skipped++
                            SmsIngestionResult.TRACKING_DISABLED -> return@withContext SmsImportOutcome(scanned, queued, duplicates, skipped)
                        }
                        scanned++
                        pageRows++
                        checkpoint = repository.inboxCheckpoint()
                    }
                }
                if (pageRows < PAGE_SIZE) break
            }
        } catch (_: SecurityException) {
            return@withContext SmsImportOutcome(scanned, queued, duplicates, skipped, permissionDenied = true)
        }
        SmsImportOutcome(scanned, queued, duplicates, skipped)
    }

    companion object {
        private const val PAGE_SIZE = 500
        private const val IMPORT_LOOKBACK_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
