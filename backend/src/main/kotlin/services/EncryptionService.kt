package org.ktugrades.backend.services

import io.ktor.utils.io.core.toByteArray
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class EncryptionService(private val key: String) {
    private val aesKey: SecretKeySpec = SecretKeySpec(key.toByteArray(), "AES")
    private val cipher: Cipher = Cipher.getInstance("AES")

    fun encrypt(text: String): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)
        return cipher.doFinal(text.toByteArray())
    }

    fun decrypt(bytes: ByteArray): String {
        cipher.init(Cipher.DECRYPT_MODE, aesKey)
        return cipher.doFinal(bytes).map { it.toChar() }.joinToString("")
    }
}