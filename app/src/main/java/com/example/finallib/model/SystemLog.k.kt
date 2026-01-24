package com.example.finallib.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SystemLog(
    val userId: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "",
    val action: String = "",
    val details: String = "",

    @ServerTimestamp
    val timestamp: Date? = null
)