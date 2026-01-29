package com.example.finallib.utils

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Log

object DRMManager {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG = "DRMManager"

    private fun getUserKey(context: Context, userId: String): SecretKey {
        return UserKeyManager.getOrCreateUserKey(context, userId)
    }

    /**
     * Format output file: [12 bytes IV][ciphertext bytes]
     */
    fun encryptFile(context: Context, inputFile: File, outputFile: File, userId: String): Boolean {
        return try {
            val key = getUserKey(context, userId)
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            cipher.updateAAD(userId.toByteArray(Charsets.UTF_8))

            FileInputStream(inputFile).use { fis ->
                FileOutputStream(outputFile).use { fos ->
                    // write IV first
                    fos.write(iv)
                    // then ciphertext
                    val buffer = ByteArray(4096)
                    var read: Int
                    val cis = cipher
                    // We'll stream via doFinal on blocks
                    // Use ByteArrayOutputStream if file small; for streaming, process blocks with update()
                    var bytesRead = fis.read(buffer)
                    while (bytesRead > 0) {
                        val out = cis.update(buffer, 0, bytesRead)
                        if (out != null && out.isNotEmpty()) fos.write(out)
                        bytesRead = fis.read(buffer)
                    }
                    val final = cis.doFinal()
                    if (final != null && final.isNotEmpty()) fos.write(final)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "encryptFile error", e)
            false
        }
    }

    /**
     * Đọc file: đầu tiên đọc IV, sau đó giải mã phần còn lại.
     */
    fun decryptFile(context: Context, encryptedFile: File, outputFile: File, userId: String): Boolean {
        return try {
            val key = getUserKey(context, userId)
            FileInputStream(encryptedFile).use { fis ->
                // read IV
                val iv = ByteArray(IV_SIZE)
                val ivRead = fis.read(iv)
                if (ivRead != IV_SIZE) throw IllegalStateException("Invalid IV in encrypted file")

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)
                cipher.updateAAD(userId.toByteArray(Charsets.UTF_8))

                FileOutputStream(outputFile).use { fos ->
                    val buffer = ByteArray(4096)
                    var read = fis.read(buffer)
                    while (read > 0) {
                        val plain = cipher.update(buffer, 0, read)
                        if (plain != null && plain.isNotEmpty()) fos.write(plain)
                        read = fis.read(buffer)
                    }
                    val final = cipher.doFinal()
                    if (final != null && final.isNotEmpty()) fos.write(final)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "decryptFile error", e)
            false
        }
    }
}
