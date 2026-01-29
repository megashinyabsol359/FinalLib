package com.example.finallib.model

import java.io.Serializable

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val language: String = "Tiếng Việt",
    val tags: List<String> = emptyList(),
    val cover: String = "",
    val url: String = "",
    val status: String = "pending",
    val uploadedAt: Long = System.currentTimeMillis(),
    val sellerId: String = "",
    val uploadedBy: String = "",
    val accessibility: String = "public",
    val price: Double = 0.0,
    val read_count: Long = 0
) : Serializable
