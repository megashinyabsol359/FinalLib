package com.example.finallib.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SellerRequest(
    val requestId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val fullName: String = "",
    val status: String = "PENDING",

    @ServerTimestamp
    val timestamp: Date? = null
)