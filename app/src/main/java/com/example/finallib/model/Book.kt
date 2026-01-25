package com.example.finallib.model

data class Book(
    val id: Int = 0,
    val title: String = "",
    val author: String = "",
    val language: String = "",
    val tags: List<String> = emptyList(),
    val cover: String = ""  // Sẽ thêm vào sau
)
