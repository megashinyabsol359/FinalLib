package com.example.finallib.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * FileDownloadService - Download file từ URL và lưu vào cache
 */
object FileDownloadService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Download file từ URL và lưu vào cache
     * @param context Android context
     * @param fileUrl URL của file cần download
     * @param fileName Tên file để lưu
     * @return Result<String> - Path đến file tạm nếu thành công
     */
    suspend fun downloadFile(
        context: Context,
        fileUrl: String,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Tạo thư mục cache cho sách
            val cacheDir = File(context.cacheDir, "books")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Tạo file tạm
            val tempFile = File(cacheDir, fileName)

            // Download file
            val request = Request.Builder()
                .url(fileUrl)
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            }

            // Lưu file
            val inputStream = response.body?.byteStream() ?: return@withContext Result.failure(
                Exception("Empty response body")
            )

            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            inputStream.close()

            if (tempFile.exists()) {
                Result.success(tempFile.absolutePath)
            } else {
                Result.failure(Exception("File not created"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xóa file tạm
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Xóa tất cả file tạm trong thư mục books cache
     */
    fun clearBookCache(context: Context): Boolean {
        return try {
            val cacheDir = File(context.cacheDir, "books")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
