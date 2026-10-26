package com.paisatrack.app.data.sms

import com.paisatrack.app.domain.model.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedSmsParserTest {
    private val parser = RuleBasedSmsParser()

    @Test fun parsesEligibleAlertIntoMinimalCandidate() {
        val result = parser.parse(envelope("Your A/c XX1234 is debited for Rs. 1,250.00 at SWIGGY on 01-Oct. Ref 123456"))
        assertEquals(125_000L, result?.amountPaise)
        assertEquals(TransactionKind.EXPENSE, result?.kind)
        assertEquals("SWIGGY", result?.merchant)
        assertEquals("123456", result?.reference)
    }

    @Test fun excludesOtpAndUnrelatedMessageContent() {
        assertNull(parser.parse(envelope("Your OTP 543210 for login is valid for 10 minutes")))
        assertNull(parser.parse(envelope("Your package worth Rs. 1,250 is out for delivery")))
        assertTrue(OtpContent.isOtpOrVerification("Never share your security code with anyone"))
        assertFalse(OtpContent.isOtpOrVerification("Your card was debited for INR 20"))
    }

    @Test fun identifiesFinancialKindsWithoutTreatingTransfersAsSpending() {
        assertEquals(TransactionKind.REFUND, parser.parse(envelope("INR 200 refund credited to account"))?.kind)
        assertEquals(TransactionKind.REVERSAL, parser.parse(envelope("Rs. 200 debited transaction reversed"))?.kind)
        assertEquals(TransactionKind.OWN_ACCOUNT_TRANSFER, parser.parse(envelope("Rs. 200 transferred between your own accounts"))?.kind)
        assertEquals(TransactionKind.CREDIT_CARD_REPAYMENT, parser.parse(envelope("Rs. 5000 credit card payment debited"))?.kind)
    }

    private fun envelope(body: String) = SmsEnvelope("HDFCBK", body, 1_800_000_000_000L)
}
