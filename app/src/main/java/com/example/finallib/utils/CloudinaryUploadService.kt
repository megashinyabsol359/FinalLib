package com.example.finallib.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "CloudinaryUpload"

/**
 * CloudinaryUploadService - Upload file HTTP thủ công
 * Không sử dụng SDK, upload trực tiếp qua REST API
 */
object CloudinaryUploadService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Upload file lên Cloudinary sử dụng unsigned preset
     * Async version sử dụng Coroutines
     */
    suspend fun uploadFile(
        context: Context,
        fileUri: Uri,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Copy file vào cache
            val tempFile = copyUriToFile(context, fileUri, fileName)

            // 2. Tạo multipart body
            val requestFile = tempFile.asRequestBody("application/octet-stream".toMediaType())
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", tempFile.name, requestFile)
                .addFormDataPart("upload_preset", CloudinaryConfig.getUploadPreset())
                .addFormDataPart("folder", "finallib/books")
                .addFormDataPart("resource_type", "auto")
                .build()

            // 3. Tạo HTTP request
            val request = Request.Builder()
                .url(CloudinaryConfig.getUploadUrl())
                .post(multipartBody)
                .build()

            Log.d(TAG, "Upload URL: ${CloudinaryConfig.getUploadUrl()}")
            Log.d(TAG, "Upload Preset: ${CloudinaryConfig.getUploadPreset()}")

            // 4. Execute request
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val secure_url = parseSecureUrl(responseBody)

                // 5. Xóa file tạm
                tempFile.delete()

                if (secure_url.isNotEmpty()) {
                    Result.success(secure_url)
                } else {
                    Result.failure(Exception("URL không hợp lệ từ response"))
                }
            } else {
                val errorBody = response.body?.string() ?: "No error body"
                Log.e(TAG, "Upload failed with code ${response.code}: $errorBody")
                Result.failure(Exception("Upload failed: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Copy file từ Uri sang file trong cache
     */
    private fun copyUriToFile(context: Context, sourceUri: Uri, fileName: String): File {
        val destinationFile = File(context.cacheDir, fileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return destinationFile
    }

    /**
     * Parse secure_url từ JSON response Cloudinary
     * Simple parsing: tìm "secure_url":"..."
     */
    private fun parseSecureUrl(jsonResponse: String): String {
        return try {
            val regex = """"secure_url":"([^"]+)"""".toRegex()
            val matchResult = regex.find(jsonResponse)
            matchResult?.groupValues?.get(1) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
