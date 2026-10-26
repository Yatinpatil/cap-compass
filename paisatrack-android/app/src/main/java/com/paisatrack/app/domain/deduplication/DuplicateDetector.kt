package com.paisatrack.app.domain.deduplication

import com.paisatrack.app.domain.model.TransactionKind
import java.security.MessageDigest
import java.util.Locale

data class TransactionFingerprintInput(
    val sender: String,
    val amountPaise: Long,
    val kind: TransactionKind,
    val occurredAtEpochMillis: Long,
    val reference: String? = null,
    val accountSuffix: String? = null,
    val merchant: String? = null,
)

fun interface FingerprintHasher {
    fun hash(canonicalValue: String): String
}

class DuplicateDetector(private val hasher: FingerprintHasher) {
    fun fingerprint(input: TransactionFingerprintInput): String {
        val reference = normalize(input.reference.orEmpty())
        val canonical = buildString {
            append(normalize(input.sender)).append('|')
            append(input.amountPaise).append('|')
            append(input.kind.name).append('|')
            append(input.occurredAtEpochMillis).append('|')
            append(normalize(input.accountSuffix.orEmpty())).append('|')
            if (reference.isNotEmpty()) append("ref:").append(reference)
            else append("merchant:").append(normalize(input.merchant.orEmpty()))
        }
        return hasher.hash(canonical)
    }

    fun isDuplicate(fingerprint: String, knownFingerprints: Set<String>): Boolean = fingerprint in knownFingerprints

    private fun normalize(value: String): String = value.trim().uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"), "")
}

object Sha256FingerprintHasher : FingerprintHasher {
    override fun hash(canonicalValue: String): String = MessageDigest.getInstance("SHA-256")
        .digest(canonicalValue.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
