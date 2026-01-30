package com.example.finallib.data

import com.example.finallib.model.User_Books
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

class UserBooksRepository {

    private val db = Firebase.firestore
    private val userBooksCollection = db.collection("user_books")

    /**
     * Retrieves a user's book list from Firestore.
     * @param userId The ID of the user.
     * @return User_Books object or null if not found.
     */
    suspend fun getUserBooks(userId: String): User_Books? {
        return try {
            userBooksCollection.document(userId).get().await().toObject(User_Books::class.java)
        } catch (e: Exception) {
            // You can log the error or handle it as needed
            null
        }
    }

    /**
     * Adds a new book to a user's purchased list.
     * If the user doesn't exist, a new document will be created.
     * If the book already exists in the list, it won't be added again.
     * @param userId The ID of the user.
     * @param bookId The ID of the book to add.
     */
    suspend fun addBookToUser(userId: String, bookId: String) {
        val userDocRef = userBooksCollection.document(userId)
        val user = getUserBooks(userId)

        if (user == null) {
            // Create a new user with the book
            val newUserBooks = User_Books(id = userId, purchasedBooks = listOf(bookId))
            userDocRef.set(newUserBooks).await()
        } else {
            // Add book to existing user, avoiding duplicates
            userDocRef.update("purchasedBooks", FieldValue.arrayUnion(bookId)).await()
        }
    }

    /**
     * Removes a book from a user's purchased list in Firestore.
     * @param userId The ID of the user.
     * @param bookId The ID of the book to remove.
     */
    suspend fun removeBookFromUser(userId: String, bookId: String) {
        try {
            val userDocRef = userBooksCollection.document(userId)
            userDocRef.update("purchasedBooks", FieldValue.arrayRemove(bookId)).await()
        } catch (e: Exception) {
            // Log or handle error
        }
    }
}
