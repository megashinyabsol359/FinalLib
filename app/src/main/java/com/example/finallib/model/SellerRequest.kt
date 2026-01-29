package com.example.finallib.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class SellerRequest(
    @DocumentId
    val id: String = "",            // ID của đơn yêu cầu
    val userId: String = "",        // ID người gửi
    val email: String = "",
    val shopName: String = "",      // Tên cửa hàng
    val bankInfo: String = "",      // <--- MỚI: Tên NH - STK - Chủ TK
    val zaloPhone: String = "",     // <--- MỚI: Số Zalo liên hệ
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val timestamp: Date = Date()
)
