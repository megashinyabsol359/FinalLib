package com.example.finallib.model

import java.io.Serializable

data class Purchase(
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val sellerId: String = "",
    val purchasedAt: Long = System.currentTimeMillis(),
    val price: Double = 0.0,
    val paymentMethod: String = "test"
) : Serializable
