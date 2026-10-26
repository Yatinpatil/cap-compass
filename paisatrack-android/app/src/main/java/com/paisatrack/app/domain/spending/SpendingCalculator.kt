package com.paisatrack.app.domain.spending

import com.paisatrack.app.domain.model.FinanceTransaction
import com.paisatrack.app.domain.model.TransactionKind
import com.paisatrack.app.domain.model.TransactionStatus
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class MonthlySpending(
    val month: YearMonth,
    val incomePaise: Long,
    val grossExpensesPaise: Long,
    val refundsPaise: Long,
    val netSpendingPaise: Long,
    val netCashFlowPaise: Long,
    val categoryNetPaise: Map<String, Long>,
)

object SpendingCalculator {
    fun calculate(
        transactions: Iterable<FinanceTransaction>,
        month: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): MonthlySpending {
        val included = transactions.asSequence()
            .filter { it.status == TransactionStatus.CONFIRMED }
            .filter { YearMonth.from(Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(zoneId)) == month }
            .toList()

        var income = 0L
        var grossExpenses = 0L
        var refunds = 0L
        var netSpending = 0L
        val byCategory = linkedMapOf<String, Long>()

        included.forEach { transaction ->
            when (transaction.kind) {
                TransactionKind.EXPENSE -> {
                    grossExpenses += transaction.amountPaise
                    netSpending += transaction.amountPaise
                    byCategory.add(transaction.categoryId, transaction.amountPaise)
                }
                TransactionKind.REFUND -> {
                    refunds += transaction.amountPaise
                    netSpending -= transaction.amountPaise
                    byCategory.add(transaction.categoryId, -transaction.amountPaise)
                }
                TransactionKind.INCOME -> income += transaction.amountPaise
                TransactionKind.REVERSAL -> when (transaction.reversesKind) {
                    TransactionKind.EXPENSE -> {
                        netSpending -= transaction.amountPaise
                        byCategory.add(transaction.categoryId, -transaction.amountPaise)
                    }
                    TransactionKind.REFUND -> {
                        refunds -= transaction.amountPaise
                        netSpending += transaction.amountPaise
                        byCategory.add(transaction.categoryId, transaction.amountPaise)
                    }
                    TransactionKind.INCOME -> income -= transaction.amountPaise
                    else -> Unit
                }
                TransactionKind.OWN_ACCOUNT_TRANSFER,
                TransactionKind.CREDIT_CARD_REPAYMENT -> Unit
            }
        }

        return MonthlySpending(
            month = month,
            incomePaise = income,
            grossExpensesPaise = grossExpenses,
            refundsPaise = refunds,
            netSpendingPaise = netSpending,
            netCashFlowPaise = income - netSpending,
            categoryNetPaise = byCategory.filterValues { it != 0L },
        )
    }

    private fun MutableMap<String, Long>.add(categoryId: String, amountPaise: Long) {
        this[categoryId] = (this[categoryId] ?: 0L) + amountPaise
    }
}
