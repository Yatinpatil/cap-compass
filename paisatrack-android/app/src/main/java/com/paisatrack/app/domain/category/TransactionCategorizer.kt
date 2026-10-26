package com.paisatrack.app.domain.category

import com.paisatrack.app.domain.model.TransactionKind

data class CategoryRule(val categoryId: String, val keywords: Set<String>)

class TransactionCategorizer(
    private val rules: List<CategoryRule> = DEFAULT_RULES,
    private val fallbackCategoryId: String = CategoryIds.OTHER,
) {
    fun categorize(merchant: String, note: String?, kind: TransactionKind): String {
        when (kind) {
            TransactionKind.OWN_ACCOUNT_TRANSFER -> return CategoryIds.TRANSFERS
            TransactionKind.CREDIT_CARD_REPAYMENT -> return CategoryIds.CARD_REPAYMENTS
            TransactionKind.INCOME -> if ("salary" in (merchant + " " + note.orEmpty()).lowercase()) return CategoryIds.INCOME
            else -> Unit
        }
        val text = "$merchant ${note.orEmpty()}".lowercase()
        return rules.firstOrNull { rule -> rule.keywords.any(text::contains) }?.categoryId ?: fallbackCategoryId
    }

    companion object {
        val DEFAULT_RULES = listOf(
            CategoryRule(CategoryIds.FOOD, setOf("swiggy", "zomato", "restaurant", "cafe", "coffee", "food")),
            CategoryRule(CategoryIds.GROCERIES, setOf("grocery", "grocer", "supermarket", "bigbasket", "blinkit", "zepto")),
            CategoryRule(CategoryIds.TRANSPORT, setOf("uber", "ola", "rapido", "metro", "fuel", "petrol", "toll")),
            CategoryRule(CategoryIds.BILLS, setOf("electricity", "bescom", "water bill", "broadband", "airtel", "jio")),
            CategoryRule(CategoryIds.SHOPPING, setOf("amazon", "flipkart", "myntra", "shopping")),
            CategoryRule(CategoryIds.HEALTH, setOf("pharmacy", "apollo", "clinic", "hospital", "medical")),
        )
    }
}

object CategoryIds {
    const val FOOD = "food_dining"
    const val GROCERIES = "groceries"
    const val TRANSPORT = "transport"
    const val BILLS = "bills_utilities"
    const val SHOPPING = "shopping"
    const val HEALTH = "health"
    const val INCOME = "income"
    const val TRANSFERS = "own_transfers"
    const val CARD_REPAYMENTS = "card_repayments"
    const val OTHER = "other"
}
