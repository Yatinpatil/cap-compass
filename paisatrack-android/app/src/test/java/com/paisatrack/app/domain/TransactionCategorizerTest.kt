package com.paisatrack.app.domain

import com.paisatrack.app.domain.category.CategoryIds
import com.paisatrack.app.domain.category.TransactionCategorizer
import com.paisatrack.app.domain.model.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionCategorizerTest {
    private val categorizer = TransactionCategorizer()

    @Test fun categorizesCommonMerchantsAndFallsBackSafely() {
        assertEquals(CategoryIds.FOOD, categorizer.categorize("Zomato order", null, TransactionKind.EXPENSE))
        assertEquals(CategoryIds.TRANSPORT, categorizer.categorize("Uber trip", null, TransactionKind.EXPENSE))
        assertEquals(CategoryIds.OTHER, categorizer.categorize("Local merchant", null, TransactionKind.EXPENSE))
    }

    @Test fun transfersAndCardRepaymentsHaveDedicatedCategories() {
        assertEquals(CategoryIds.TRANSFERS, categorizer.categorize("own accounts", null, TransactionKind.OWN_ACCOUNT_TRANSFER))
        assertEquals(CategoryIds.CARD_REPAYMENTS, categorizer.categorize("bank payment", null, TransactionKind.CREDIT_CARD_REPAYMENT))
        assertEquals(CategoryIds.INCOME, categorizer.categorize("Monthly salary", null, TransactionKind.INCOME))
    }
}
