package com.example.finallib.utils

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object UserKeyManager {

    private const val PREFS_FILE = "user_keys_prefs"
    private const val KEY_PREFIX = "key_"
    private const val KEY_SIZE_BYTES = 32 // 256-bit

    private fun getMasterKey(context: Context): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun getPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            getMasterKey(context),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    /**
     * Trả về SecretKey AES-256 cho userId. Nếu chưa có thì tạo mới và lưu (Base64) trong EncryptedSharedPreferences.
     */
    fun getOrCreateUserKey(context: Context, userId: String): SecretKey {
        val prefs = getPrefs(context)
        val keyName = KEY_PREFIX + userId
        val existing = prefs.getString(keyName, null)
        return if (existing != null) {
            val bytes = Base64.decode(existing, Base64.DEFAULT)
            SecretKeySpec(bytes, "AES")
        } else {
            val rnd = SecureRandom()
            val keyBytes = ByteArray(KEY_SIZE_BYTES)
            rnd.nextBytes(keyBytes)
            val encoded = Base64.encodeToString(keyBytes, Base64.NO_WRAP)
            prefs.edit().putString(keyName, encoded).apply()
            SecretKeySpec(keyBytes, "AES")
        }
    }
}
