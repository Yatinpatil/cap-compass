package com.paisatrack.app.domain

import com.paisatrack.app.domain.money.MoneyParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyParserTest {
    @Test fun parsesRupeesExactlyToIntegerPaise() {
        assertEquals(123_456L, MoneyParser.rupeesToPaise("₹1,234.56"))
        assertEquals(1L, MoneyParser.rupeesToPaise("0.01"))
        assertEquals(20_000L, MoneyParser.rupeesToPaise("INR 200"))
    }

    @Test fun rejectsInvalidPrecisionAndNonPositiveAmounts() {
        assertThrows(IllegalArgumentException::class.java) { MoneyParser.rupeesToPaise("1.001") }
        assertThrows(IllegalArgumentException::class.java) { MoneyParser.rupeesToPaise("0") }
        assertThrows(IllegalArgumentException::class.java) { MoneyParser.rupeesToPaise("not money") }
    }
}
