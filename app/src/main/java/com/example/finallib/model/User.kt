package com.example.finallib.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "User",
)