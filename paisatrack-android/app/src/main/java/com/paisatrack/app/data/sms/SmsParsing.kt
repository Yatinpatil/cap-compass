package com.paisatrack.app.data.sms

import com.paisatrack.app.domain.money.MoneyParser
import com.paisatrack.app.domain.model.TransactionKind

/** Raw body is a short-lived input only; candidates never carry message text. */
data class SmsEnvelope(
    val senderAddress: String,
    val body: String,
    val receivedAtEpochMillis: Long,
    val providerMessageId: Long? = null,
)

data class SmsTransactionCandidate(
    val amountPaise: Long,
    val kind: TransactionKind,
    val merchant: String,
    val transactionTimeEpochMillis: Long,
    val reference: String?,
    val accountSuffix: String?,
    val reversesKind: TransactionKind?,
)

fun interface SmsParser {
    fun parse(envelope: SmsEnvelope): SmsTransactionCandidate?
}

fun interface SmsEligibilityFilter {
    fun isEligible(senderAddress: String, body: String): Boolean
}

class BankAlertEligibilityFilter : SmsEligibilityFilter {
    override fun isEligible(senderAddress: String, body: String): Boolean {
        if (OtpContent.isOtpOrVerification(body)) return false
        if (!HAS_CURRENCY_AMOUNT.containsMatchIn(body)) return false
        if (!TRANSACTION_ACTION.containsMatchIn(body)) return false
        val sender = senderAddress.uppercase()
        val senderLooksFinancial = FINANCIAL_SENDER_HINTS.any(sender::contains)
        return senderLooksFinancial || FINANCIAL_BODY_HINTS.any { body.contains(it, ignoreCase = true) }
    }

    companion object {
        private val HAS_CURRENCY_AMOUNT = Regex("(?i)(?:₹|INR|RS\\.?)\\s*[0-9][0-9,]*(?:\\.[0-9]{1,2})?")
        private val TRANSACTION_ACTION = Regex("(?i)\\b(debited|credited|withdrawn|spent|purchase|paid|refund|reversal|reversed|transferred|received|deposit)\\b")
        private val FINANCIAL_SENDER_HINTS = listOf("BANK", "HDFC", "ICICI", "AXIS", "SBI", "KOTAK", "IDFC", "PNB", "YESBANK", "PAYTM", "PHONEPE", "GPAY", "UPI", "VISA", "MASTERCARD", "BESCOM")
        private val FINANCIAL_BODY_HINTS = listOf("a/c", "account", "bank", "upi", "card", "wallet", "transaction", "txn", "balance")
    }
}

class RuleBasedSmsParser(
    private val eligibility: SmsEligibilityFilter = BankAlertEligibilityFilter(),
) : SmsParser {
    override fun parse(envelope: SmsEnvelope): SmsTransactionCandidate? {
        val body = envelope.body
        if (!eligibility.isEligible(envelope.senderAddress, body)) return null
        val amountText = CURRENCY_AMOUNT.find(body)?.groupValues?.drop(1)?.firstOrNull(String::isNotBlank) ?: return null
        val amountPaise = runCatching { MoneyParser.rupeesToPaise(amountText) }.getOrNull() ?: return null
        val kind = transactionKind(body) ?: return null
        val reversedKind = if (kind == TransactionKind.REVERSAL) {
            when {
                Regex("(?i)\\b(salary|credited|deposit)\\b").containsMatchIn(body) -> TransactionKind.INCOME
                Regex("(?i)\\b(refund)\\b").containsMatchIn(body) -> TransactionKind.REFUND
                else -> TransactionKind.EXPENSE
            }
        } else null

        return SmsTransactionCandidate(
            amountPaise = amountPaise,
            kind = kind,
            merchant = merchantFrom(body),
            transactionTimeEpochMillis = envelope.receivedAtEpochMillis,
            reference = REFERENCE.find(body)?.groupValues?.getOrNull(1),
            accountSuffix = ACCOUNT_SUFFIX.find(body)?.groupValues?.getOrNull(1),
            reversesKind = reversedKind,
        )
    }

    private fun transactionKind(body: String): TransactionKind? {
        val normalized = body.lowercase()
        return when {
            Regex("\\b(reversed|reversal|chargeback)\\b").containsMatchIn(normalized) -> TransactionKind.REVERSAL
            Regex("\\b(refund|refunded)\\b").containsMatchIn(normalized) -> TransactionKind.REFUND
            Regex("credit card (payment|bill|repayment)|card payment (?:received|made)").containsMatchIn(normalized) -> TransactionKind.CREDIT_CARD_REPAYMENT
            Regex("own account|between your accounts|self transfer").containsMatchIn(normalized) -> TransactionKind.OWN_ACCOUNT_TRANSFER
            Regex("\\b(credited|salary|received|deposit)\\b").containsMatchIn(normalized) -> TransactionKind.INCOME
            Regex("\\b(debited|withdrawn|spent|purchase|paid|transferred)\\b").containsMatchIn(normalized) -> TransactionKind.EXPENSE
            else -> null
        }
    }

    private fun merchantFrom(body: String): String {
        val raw = MERCHANT.find(body)?.groupValues?.getOrNull(1) ?: return "Bank transaction"
        val stop = MERCHANT_END.find(raw)?.range?.first ?: raw.length
        val match = raw.take(stop).trim(' ', '.', ',', ':', ';', '-').take(48)
        return match?.takeIf { it.length >= 2 } ?: "Bank transaction"
    }

    companion object {
        private val CURRENCY_AMOUNT = Regex("(?i)(?:₹|INR|RS\\.?)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)")
        private val MERCHANT = Regex("(?i)\\b(?:at|to|towards|from)\\s+([A-Za-z0-9][A-Za-z0-9 &.'_-]{1,60})")
        private val MERCHANT_END = Regex("(?i)\\b(?:on|at|ref|txn|avl|available|a/c|account)\\b")
        private val REFERENCE = Regex("(?i)\\b(?:ref(?:erence)?(?:\\s*(?:no|id))?|txn(?:\\s*id)?)[:\\s#-]*([A-Z0-9-]{5,28})")
        private val ACCOUNT_SUFFIX = Regex("(?i)\\b(?:a/c|account|card)\\b[^\\n]{0,30}?(?:x{2,}|\\*{2,})?([0-9]{4})\\b")
    }
}

object OtpContent {
    private val OTP_TERMS = Regex("(?i)\\b(otp|one[- ]time password|verification code|security code)\\b")
    private val DO_NOT_SHARE = Regex("(?i)(do not share|never share|don't share).{0,40}\\b(code|otp|password)\\b")

    fun isOtpOrVerification(body: String): Boolean = OTP_TERMS.containsMatchIn(body) || DO_NOT_SHARE.containsMatchIn(body)
}
