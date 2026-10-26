package com.paisatrack.app.domain

import com.paisatrack.app.domain.model.FinanceTransaction
import com.paisatrack.app.domain.model.TransactionKind
import com.paisatrack.app.domain.model.TransactionSource
import com.paisatrack.app.domain.model.TransactionStatus
import com.paisatrack.app.domain.spending.SpendingCalculator
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class SpendingCalculatorTest {
    private val month = YearMonth.of(2026, 10)
    private val time = Instant.parse("2026-10-15T12:00:00Z").toEpochMilli()
    private val zone = ZoneOffset.UTC

    @Test fun refundsAndReversalsAdjustOnlyTheirOriginalKinds() {
        val rows = listOf(
            tx("expense", 10_000, TransactionKind.EXPENSE, category = "food"),
            tx("refund", 2_000, TransactionKind.REFUND, category = "food"),
            tx("expense-reversal", 3_000, TransactionKind.REVERSAL, reverses = TransactionKind.EXPENSE, category = "food"),
            tx("refund-reversal", 500, TransactionKind.REVERSAL, reverses = TransactionKind.REFUND, category = "food"),
            tx("income", 50_000, TransactionKind.INCOME),
            tx("income-reversal", 5_000, TransactionKind.REVERSAL, reverses = TransactionKind.INCOME),
            tx("transfer", 25_000, TransactionKind.OWN_ACCOUNT_TRANSFER),
            tx("card-payment", 12_000, TransactionKind.CREDIT_CARD_REPAYMENT),
            tx("pending", 99_000, TransactionKind.EXPENSE, status = TransactionStatus.PENDING_REVIEW),
            tx("other-month", 77_000, TransactionKind.EXPENSE, time = Instant.parse("2026-09-30T23:59:59Z").toEpochMilli()),
        )
        val result = SpendingCalculator.calculate(rows, month, zone)
        assertEquals(45_000L, result.incomePaise)
        assertEquals(10_000L, result.grossExpensesPaise)
        assertEquals(1_500L, result.refundsPaise)
        assertEquals(5_500L, result.netSpendingPaise)
        assertEquals(39_500L, result.netCashFlowPaise)
        assertEquals(5_500L, result.categoryNetPaise["food"])
    }

    private fun tx(
        id: String,
        paise: Long,
        kind: TransactionKind,
        category: String = "other",
        status: TransactionStatus = TransactionStatus.CONFIRMED,
        reverses: TransactionKind? = null,
        time: Long = this.time,
    ) = FinanceTransaction(id, paise, kind, status, id, categoryId = category, occurredAtEpochMillis = time, source = TransactionSource.MANUAL, reversesKind = reverses)
}
