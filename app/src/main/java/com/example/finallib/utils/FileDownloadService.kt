package com.example.finallib.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * FileDownloadService - Download file từ URL và lưu vào thư mục files (persistent per app)
 */
object FileDownloadService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Download file từ URL và lưu vào thư mục files (persistent).
     * Trả về đường dẫn file đã được mã hóa (.enc) — encrypted per userId
     */
    suspend fun downloadFile(
        context: Context,
        fileUrl: String,
        fileName: String,
        userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Lưu vào internal files để giữ lâu dài (private của app)
            val storageDir = File(context.filesDir, "books")
            if (!storageDir.exists()) storageDir.mkdirs()

            // Tạo file tạm plaintext trong cache khi download (an toàn hơn)
            val tempFile = File(context.cacheDir, fileName)

            val request = Request.Builder().url(fileUrl).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Download failed: ${response.code}"))

            val inputStream = response.body?.byteStream() ?: return@withContext Result.failure(Exception("Empty response body"))
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()

            if (!tempFile.exists()) return@withContext Result.failure(Exception("File not created"))

            // Encrypt with user-specific key, store encrypted file persistently in filesDir
            val encFile = File(storageDir, "$fileName.enc")
            val encryptedOk = DRMManager.encryptFile(context, tempFile, encFile, userId)

            // Delete plaintext temp
            try { tempFile.delete() } catch (_: Exception) {}

            if (!encryptedOk || !encFile.exists()) {
                return@withContext Result.failure(Exception("DRM encryption failed"))
            }

            Result.success(encFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Xoá toàn bộ storage books trong filesDir (xóa sách persistent)
     */
    fun clearPersistentBooks(context: Context): Boolean {
        return try {
            val storageDir = File(context.filesDir, "books")
            if (storageDir.exists()) storageDir.deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Xóa tất cả file tạm trong thư mục books cache (nếu có)
     */
    fun clearBookCache(context: Context): Boolean {
        return try {
            val cacheDir = File(context.cacheDir, "books")
            if (cacheDir.exists()) cacheDir.deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }
}
