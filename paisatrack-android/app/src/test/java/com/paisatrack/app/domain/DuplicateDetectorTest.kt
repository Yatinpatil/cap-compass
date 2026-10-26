package com.paisatrack.app.domain

import com.paisatrack.app.domain.deduplication.DuplicateDetector
import com.paisatrack.app.domain.deduplication.Sha256FingerprintHasher
import com.paisatrack.app.domain.deduplication.TransactionFingerprintInput
import com.paisatrack.app.domain.model.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDetectorTest {
    @Test fun canonicalFingerprintIgnoresFormattingNoiseAndDetectsChanges() {
        val detector = DuplicateDetector(Sha256FingerprintHasher)
        val first = detector.fingerprint(input(sender = "HDFC-BANK", reference = "AB-12345"))
        val normalized = detector.fingerprint(input(sender = "hdfc bank", reference = "ab12345"))
        val different = detector.fingerprint(input(sender = "HDFC-BANK", reference = "AB-99999"))
        assertEquals(first, normalized)
        assertNotEquals(first, different)
        assertTrue(detector.isDuplicate(first, setOf(first)))
    }

    private fun input(sender: String, reference: String) = TransactionFingerprintInput(sender, 12_300, TransactionKind.EXPENSE, 1_800_000_000_000, reference = reference, accountSuffix = "1234")
}
