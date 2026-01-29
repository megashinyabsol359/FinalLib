package com.example.finallib.model

import com.google.firebase.firestore.DocumentId

data class User_Books(
    @DocumentId
    val id: String = "",
    val purchasedBooks: List<String> = emptyList()
)