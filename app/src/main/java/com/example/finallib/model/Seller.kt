package com.example.finallib.model

import com.google.firebase.firestore.DocumentId

data class Seller(
    @DocumentId
    val userId: String = "",     // Dùng luôn ID của User làm ID của Seller
    val shopName: String = "",   // Tên cửa hàng
    val bankInfo: String = "",   // Thông tin ngân hàng (để Admin chuyển tiền)
    val zaloPhone: String = "",  // Số Zalo liên hệ
    val totalRevenue: Long = 0,  // Tổng doanh thu (Lưu dạng Long để tránh sai số tiền tệ)
    val isActive: Boolean = true // Trạng thái hoạt động (True: Đang bán, False: Bị khóa)
)
