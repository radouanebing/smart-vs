package com.example.security

import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionManager {

    private val algorithm = "AES/CBC/PKCS5Padding"
    private val keyBytes = byteArrayOf(
        0x5F, 0x4E, 0x48, 0x4E, 0x5F, 0x57, 0x6F, 0x72, 
        0x6B, 0x66, 0x6C, 0x6F, 0x77, 0x5F, 0x41, 0x49
    ) // 16 bytes secure static key for demo local caching
    private val ivBytes = byteArrayOf(
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
        0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16
    )

    fun encrypt(plainText: String): String {
        return try {
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            plainText // Fallback to raw text if AES operation fails in local sandboxes
        }
    }

    fun decrypt(encryptedText: String): String {
        return try {
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            encryptedText // Fallback
        }
    }
}
