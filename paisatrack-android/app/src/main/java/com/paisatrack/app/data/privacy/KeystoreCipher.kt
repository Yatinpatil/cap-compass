package com.paisatrack.app.data.privacy

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.paisatrack.app.domain.deduplication.FingerprintHasher
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts sensitive Room columns with AES-GCM keys that never leave Android Keystore. */
object KeystoreCipher {
    private const val PROVIDER = "AndroidKeyStore"
    private const val AES_ALIAS = "paisa_track_room_aes_v1"
    private const val HMAC_ALIAS = "paisa_track_sms_hmac_v1"
    private const val PREFIX = "v1:"
    private val lock = Any()

    fun encrypt(plaintext: String): String = synchronized(lock) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getAesKey())
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        PREFIX + Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String = synchronized(lock) {
        require(encoded.startsWith(PREFIX)) { "Unsupported protected value version." }
        val payload = Base64.decode(encoded.removePrefix(PREFIX), Base64.NO_WRAP)
        require(payload.size > GCM_IV_LENGTH_BYTES) { "Protected value is incomplete." }
        val iv = payload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val encrypted = payload.copyOfRange(GCM_IV_LENGTH_BYTES, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getAesKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun fingerprint(canonicalValue: String): String = synchronized(lock) {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(getHmacKey())
        mac.doFinal(canonicalValue.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun deleteKeys() = synchronized(lock) {
        val keyStore = loadKeyStore()
        listOf(AES_ALIAS, HMAC_ALIAS).forEach { alias -> if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias) }
    }

    private fun getAesKey(): SecretKey {
        (loadKeyStore().getKey(AES_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(AES_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private fun getHmacKey(): SecretKey {
        (loadKeyStore().getKey(HMAC_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(HMAC_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build(),
            )
            generateKey()
        }
    }

    private fun loadKeyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
}

class KeystoreFingerprintHasher : FingerprintHasher {
    override fun hash(canonicalValue: String): String = KeystoreCipher.fingerprint(canonicalValue)
}
