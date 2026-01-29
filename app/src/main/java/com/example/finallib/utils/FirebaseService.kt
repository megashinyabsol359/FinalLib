package com.example.finallib.utils

import com.example.finallib.model.Book
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object FirebaseService {

    private val db = FirebaseFirestore.getInstance()
    private const val BOOKS_COLLECTION = "books"

    /**
     * Lưu sách mới lên Firestore với status "pending"
     */
    suspend fun uploadBookData(book: Book): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val newBook = book.copy(
                status = "pending",  // Mặc định là pending
                uploadedAt = System.currentTimeMillis()
            )

            db.collection(BOOKS_COLLECTION)
                .add(newBook)
                .addOnSuccessListener { documentReference ->
                    val docId = documentReference.id
                    // Cập nhật ID của document
                    documentReference.update("id", docId)
                        .addOnSuccessListener {
                            continuation.resume(Result.success(docId))
                        }
                        .addOnFailureListener { e ->
                            continuation.resumeWithException(e)
                        }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Lấy tất cả sách từ Firestore
     */
    suspend fun getAllBooks(): Result<List<Book>> = suspendCancellableCoroutine { continuation ->
        try {
            db.collection(BOOKS_COLLECTION)
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        val books = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Book::class.java)
                        }
                        continuation.resume(Result.success(books))
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Lấy sách theo ID
     */
    suspend fun getBookById(bookId: String): Result<Book?> = suspendCancellableCoroutine { continuation ->
        try {
            db.collection(BOOKS_COLLECTION)
                .document(bookId)
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        val book = snapshot.toObject(Book::class.java)
                        continuation.resume(Result.success(book))
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Cập nhật status sách (pending -> approved/rejected)
     */
    suspend fun updateBookStatus(bookId: String, newStatus: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            db.collection(BOOKS_COLLECTION)
                .document(bookId)
                .update("status", newStatus)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Xóa sách từ Firestore
     */
    suspend fun deleteBook(bookId: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            db.collection(BOOKS_COLLECTION)
                .document(bookId)
                .delete()
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Lấy sách theo status
     */
    suspend fun getBooksByStatus(status: String): Result<List<Book>> = suspendCancellableCoroutine { continuation ->
        try {
            db.collection(BOOKS_COLLECTION)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        val books = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Book::class.java)
                        }
                        continuation.resume(Result.success(books))
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Lấy sách của người bán theo sellerId
     */
    suspend fun getBooksBySeller(sellerId: String): Result<List<Book>> = suspendCancellableCoroutine { continuation ->
        try {
            db.collection(BOOKS_COLLECTION)
                .whereEqualTo("sellerId", sellerId)
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        val books = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Book::class.java)
                        }
                        continuation.resume(Result.success(books))
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}
