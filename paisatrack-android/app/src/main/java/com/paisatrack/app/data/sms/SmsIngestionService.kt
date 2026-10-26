package com.paisatrack.app.data.sms

import com.paisatrack.app.data.repository.FinanceRepository
import com.paisatrack.app.domain.category.TransactionCategorizer
import com.paisatrack.app.domain.deduplication.TransactionFingerprintInput
import com.paisatrack.app.domain.model.FinanceTransaction
import com.paisatrack.app.domain.model.TransactionSource
import com.paisatrack.app.domain.model.TransactionStatus
import java.util.UUID

enum class SmsIngestionResult { QUEUED_FOR_REVIEW, DUPLICATE, SKIPPED_INELIGIBLE, TRACKING_DISABLED }

class SmsIngestionService(
    private val repository: FinanceRepository,
    private val parser: SmsParser = RuleBasedSmsParser(),
    private val categorizer: TransactionCategorizer = TransactionCategorizer(),
) {
    suspend fun process(envelope: SmsEnvelope): SmsIngestionResult {
        if (!repository.smsImportEnabled()) return SmsIngestionResult.TRACKING_DISABLED
        val parsed = parser.parse(envelope) ?: run {
            envelope.providerMessageId?.let { repository.advanceInboxCheckpoint(envelope.receivedAtEpochMillis, it) }
            return SmsIngestionResult.SKIPPED_INELIGIBLE
        }
        val categoryId = categorizer.categorize(parsed.merchant, null, parsed.kind)
        val candidate = FinanceTransaction(
            id = UUID.randomUUID().toString(),
            amountPaise = parsed.amountPaise,
            kind = parsed.kind,
            status = TransactionStatus.PENDING_REVIEW,
            merchant = parsed.merchant,
            categoryId = categoryId,
            occurredAtEpochMillis = parsed.transactionTimeEpochMillis,
            source = TransactionSource.SMS,
            reversesKind = parsed.reversesKind,
        )
        val fingerprintInput = TransactionFingerprintInput(
            sender = envelope.senderAddress,
            amountPaise = parsed.amountPaise,
            kind = parsed.kind,
            occurredAtEpochMillis = parsed.transactionTimeEpochMillis,
            reference = parsed.reference,
            accountSuffix = parsed.accountSuffix,
            merchant = parsed.merchant,
        )
        val stored = repository.storeSmsCandidate(
            candidate = candidate,
            fingerprintInput = fingerprintInput,
            providerMessageDate = envelope.providerMessageId?.let { envelope.receivedAtEpochMillis },
            providerMessageId = envelope.providerMessageId,
        )
        return if (stored) SmsIngestionResult.QUEUED_FOR_REVIEW else SmsIngestionResult.DUPLICATE
    }
}
