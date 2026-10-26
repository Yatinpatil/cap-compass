package com.paisatrack.app.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.paisatrack.app.BuildConfig
import com.paisatrack.app.PaisaTrackApplication
import com.paisatrack.app.data.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class IncomingSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!BuildConfig.SMS_IMPORT_AVAILABLE || intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        val app = context.applicationContext as? PaisaTrackApplication
        if (app == null) {
            pending.finish()
            return
        }

        val messages = parseMessages(intent)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val service = app.container.smsIngestion
                messages.groupBy { it.originatingAddress.orEmpty() to it.timestampMillis }
                    .forEach { (senderAndTimestamp, parts) ->
                        service.process(
                            SmsEnvelope(
                                senderAddress = senderAndTimestamp.first,
                                body = parts.joinToString(separator = "") { it.messageBody.orEmpty() },
                                receivedAtEpochMillis = senderAndTimestamp.second,
                            ),
                        )
                    }
            } catch (_: SecurityException) {
                app.container.financeRepository.savePreference(FinanceRepository.KEY_SMS_PERMISSION, "revoked")
                app.container.financeRepository.savePreference(FinanceRepository.KEY_SMS_TRACKING, "false")
            } finally {
                pending.finish()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun parseMessages(intent: Intent): List<SmsMessage> {
        val pdus = intent.extras?.get("pdus") as? Array<*> ?: return emptyList()
        val format = intent.getStringExtra("format")
        return pdus.mapNotNull { pdu ->
            (pdu as? ByteArray)?.let { bytes ->
                if (format == null) SmsMessage.createFromPdu(bytes) else SmsMessage.createFromPdu(bytes, format)
            }
        }
    }
}
