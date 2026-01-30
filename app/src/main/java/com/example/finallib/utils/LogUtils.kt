package com.example.finallib.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

object LogUtils {
    private const val COLLECTION_LOGS = "system_logs"

    fun writeLog(action: String, details: String) {
        val user = FirebaseAuth.getInstance().currentUser
        
        // Dữ liệu Log
        val logData = hashMapOf(
            "userId" to (user?.uid ?: "Anonymous"),
            "email" to (user?.email ?: "Unknown"), 
            "action" to action,
            "details" to details,
            "timestamp" to Date(), 
            "fullName" to (user?.displayName ?: ""), 
            "role" to "" 
        )

        FirebaseFirestore.getInstance()
            .collection(COLLECTION_LOGS)
            .add(logData)
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}
