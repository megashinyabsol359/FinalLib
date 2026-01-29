package com.example.finallib.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SystemLog(
    @DocumentId
    val id: String = "",          // Thêm cái này để quản lý ID document
    val userId: String = "",
    val email: String = "",       // Đồng bộ tên này với LogUtils
    val fullName: String = "",    // LogUtils sẽ để trống trường này (hoặc "Unknown")
    val role: String = "",        // LogUtils sẽ để trống trường này
    val action: String = "",
    val details: String = "",

    @ServerTimestamp
    val timestamp: Date? = null
)
