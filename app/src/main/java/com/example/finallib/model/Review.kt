package com.example.finallib.model

import java.io.Serializable
import java.util.Date

data class Review(
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Date = Date()
) : Serializable
