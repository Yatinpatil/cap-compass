package com.paisatrack.app.domain.money

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

object FinanceDateParser {
    private val inputFormatter = DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)

    fun parseDateAtStartOfDay(input: String, zoneId: ZoneId = ZoneId.systemDefault()): Long = try {
        LocalDate.parse(input.trim(), inputFormatter).atStartOfDay(zoneId).toInstant().toEpochMilli()
    } catch (_: Exception) {
        throw IllegalArgumentException("Enter a real date as day/month/year, for example 5/10/2026.")
    }

    fun formatDate(epochMillis: Long, pattern: String = "d MMM yyyy", zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag("en-IN")))
}
