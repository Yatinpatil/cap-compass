package com.paisatrack.app.domain.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object MoneyParser {
    /** Parses rupee text exactly into integer paise; it never uses binary floating point. */
    fun rupeesToPaise(input: String): Long {
        val normalized = input.trim()
            .replace("₹", "")
            .replace(",", "")
            .replace(Regex("(?i)\\bINR\\b|\\bRs\\.?\\s*"), "")
            .trim()
        require(normalized.isNotBlank()) { "Enter an amount." }
        val decimal = try {
            BigDecimal(normalized)
        } catch (_: NumberFormatException) {
            throw IllegalArgumentException("Enter a valid rupee amount.")
        }
        require(decimal > BigDecimal.ZERO) { "Amount must be greater than zero." }
        return try {
            decimal.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("Use no more than two decimal places.")
        }
    }

    fun formatPaise(amountPaise: Long, locale: Locale = Locale.forLanguageTag("en-IN")): String {
        val rupees = BigDecimal.valueOf(amountPaise, 2)
        return NumberFormat.getCurrencyInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(rupees)
    }
}
