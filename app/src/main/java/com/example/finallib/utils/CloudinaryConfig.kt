package com.example.finallib.utils

import android.content.Context
import java.util.Properties

/**
 * CloudinaryConfig - Quản lý thông tin cấu hình Cloudinary
 * Đọc từ file cloudinary.properties (không được commit)
 */
object CloudinaryConfig {
    
    private var cloudName: String = ""
    private var uploadPreset: String = ""
    private var uploadUrl: String = ""
    
    private var isInitialized = false
    
    /**
     * Khởi tạo config từ file cloudinary.properties
     * Gọi một lần trong MainActivity.onCreate()
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        try {
            val properties = Properties()
            // Đọc từ assets/cloudinary.properties
            val assetManager = context.assets
            val inputStream = assetManager.open("cloudinary.properties")
            properties.load(inputStream)
            inputStream.close()
            
            cloudName = properties.getProperty("cloudinary.cloudName", "")
            uploadPreset = properties.getProperty("cloudinary.uploadPreset", "")
            uploadUrl = properties.getProperty("cloudinary.uploadUrl", "")
                .replace("{cloudName}", cloudName)
            
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: sử dụng BuildConfig nếu có
            try {
                cloudName = "YOUR_CLOUD_NAME" // Có thể load từ BuildConfig
                uploadPreset = "YOUR_UNSIGNED_PRESET"
                uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/auto/upload"
                isInitialized = true
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
    
    /**
     * Lấy Cloud Name từ config
     */
    fun getCloudName(): String = cloudName
    
    /**
     * Lấy Upload Preset từ config
     */
    fun getUploadPreset(): String = uploadPreset
    
    /**
     * Lấy Upload URL từ config
     */
    fun getUploadUrl(): String = uploadUrl
    
    /**
     * Kiểm tra xem config đã được khởi tạo hay chưa
     */
    fun isInitialized(): Boolean = isInitialized
}
