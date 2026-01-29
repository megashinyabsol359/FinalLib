package com.example.finallib.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

object LogUtils {
    // Tên collection trên Firebase
    private const val COLLECTION_LOGS = "system_logs"

    /**
     * @param action: Tên hành động ngắn gọn (Ví dụ: "LOGIN", "READ_BOOK", "BUY_BOOK")
     * @param details: Mô tả chi tiết (Ví dụ: "Đã nạp 50k", "Đang đọc sách ABC")
     */
    fun writeLog(action: String, details: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) return // Chưa đăng nhập thì không ghi log (hoặc ghi log ẩn danh tùy bạn)

        val logData = hashMapOf(
            "userId" to user.uid,
            "userEmail" to (user.email ?: "Unknown"),
            "action" to action,
            "details" to details,
            "timestamp" to Date() // Lấy thời gian hiện tại
        )

        FirebaseFirestore.getInstance()
            .collection(COLLECTION_LOGS)
            .add(logData)
            .addOnFailureListener { e ->
                e.printStackTrace() // In lỗi nếu ghi log thất bại (để debug)
            }
    }
}
