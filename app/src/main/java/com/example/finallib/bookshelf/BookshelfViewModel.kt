/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.example.finallib.bookshelf

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finallib.data.UserBooksRepository
import com.example.finallib.data.model.Book
import com.example.finallib.domain.Bookshelf
import com.example.finallib.reader.OpeningError
import com.example.finallib.reader.ReaderActivityContract
import com.example.finallib.utils.EventChannel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.toUrl
import java.util.concurrent.ConcurrentHashMap
import timber.log.Timber
import java.io.File

class BookshelfViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() =
        getApplication<com.example.finallib.main.Application>()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val userBooksRepository = UserBooksRepository()

    val channel = EventChannel(Channel<Event>(Channel.BUFFERED), viewModelScope)
    
    private val remotePurchasedBooks = MutableStateFlow<List<Book>>(emptyList())
    private val purchasedBookIds = MutableStateFlow<Set<String>>(emptySet())
    
    private val isStoreBookCache = ConcurrentHashMap<String, Boolean>()
    private val cacheUpdateTrigger = MutableStateFlow(0)

    // Only show books that are actually downloaded (in local database)
    val books: StateFlow<List<Book>> = app.bookRepository.books()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshPurchasedBooks()
    }

    fun refreshPurchasedBooks() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                try {
                    val userBooks = userBooksRepository.getUserBooks(userId)
                    val ids = userBooks?.purchasedBooks?.toSet() ?: emptySet()
                    purchasedBookIds.value = ids
                    
                    if (ids.isNotEmpty()) {
                        fetchRemoteBookDetails(ids.toList())
                    } else {
                        remotePurchasedBooks.value = emptyList()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error refreshing purchased books")
                }
            }
        } else {
            purchasedBookIds.value = emptySet()
            remotePurchasedBooks.value = emptyList()
        }
    }

    private suspend fun fetchRemoteBookDetails(ids: List<String>) {
        try {
            val booksList = mutableListOf<Book>()
            val documents = db.collection("books")
                .whereIn(FieldPath.documentId(), ids)
                .get()
                .await()

            for (doc in documents) {
                val firestoreBook = doc.toObject(com.example.finallib.model.Book::class.java)
                val bookId = if (firestoreBook.id.isNotEmpty()) firestoreBook.id else doc.id
                
                val book = Book(
                    id = null, 
                    title = firestoreBook.title,
                    author = firestoreBook.author,
                    href = firestoreBook.url, 
                    identifier = bookId,
                    cover = firestoreBook.cover, 
                    rawMediaType = "application/epub+zip"
                )
                booksList.add(book)
                isStoreBookCache[book.identifier] = true
            }
            remotePurchasedBooks.value = booksList
            cacheUpdateTrigger.value += 1
        } catch (e: Exception) {
            Timber.e(e, "Error fetching remote book details")
        }
    }

    private fun checkIfStoreBook(identifier: String) {
        viewModelScope.launch {
            if (isStoreBookCache.containsKey(identifier) || identifier.isEmpty()) return@launch
            
            try {
                val doc = db.collection("books").document(identifier).get().await()
                isStoreBookCache[identifier] = doc.exists()
            } catch (e: Exception) {
                isStoreBookCache[identifier] = false
            }
            cacheUpdateTrigger.value += 1
        }
    }

    fun deletePublication(book: Book) =
        viewModelScope.launch {
            // 1. Delete from local database and filesystem
            if (book.id != null) {
                app.bookshelf.deleteBook(book)
            }

            // 2. Remove from Firestore purchased list so it doesn't reappear
            val userId = auth.currentUser?.uid
            if (userId != null && book.identifier.isNotEmpty()) {
                userBooksRepository.removeBookFromUser(userId, book.identifier)
                // Refresh to update local state
                refreshPurchasedBooks()
            }
            
            // If it was a purely remote book (though the 'books' flow now filters them out)
            if (book.id == null) {
                remotePurchasedBooks.value = remotePurchasedBooks.value.filter { it.identifier != book.identifier }
            }
        }

    fun importPublicationFromStorage(uri: Uri) {
        app.bookshelf.importPublicationFromStorage(uri)
    }

    fun addPublicationFromStorage(uri: Uri) {
        app.bookshelf.addPublicationFromStorage(uri.toUrl()!! as AbsoluteUrl)
    }

    fun addPublicationFromWeb(url: AbsoluteUrl) {
        app.bookshelf.addPublicationFromWeb(url)
    }

    fun openPublication(book: Book) {
        if (book.id != null) {
            openLocalPublication(book.id!!)
        } else {
            viewModelScope.launch {
                val localBook = app.bookRepository.books().first().find { 
                    it.identifier == book.identifier || 
                    (it.title == book.title && it.author == book.author)
                }
                if (localBook != null) {
                    openLocalPublication(localBook.id!!)
                } else {
                    channel.send(Event.LaunchRemoteReader(book))
                }
            }
        }
    }

    fun importDownloadedBook(book: Book, filePath: String) {
        viewModelScope.launch {
            val file = File(filePath)
            val url = file.toUrl()
            app.bookshelf.addPublication(url as AbsoluteUrl)
                .onSuccess { bookId ->
                    openLocalPublication(bookId)
                }
                .onFailure {
                    Timber.e("Failed to import downloaded book: $it")
                }
        }
    }

    private fun openLocalPublication(bookId: Long) {
        viewModelScope.launch {
            app.readerRepository
                .open(bookId)
                .onFailure {
                    channel.send(Event.OpenPublicationError(it))
                }
                .onSuccess {
                    val arguments = ReaderActivityContract.Arguments(bookId)
                    channel.send(Event.LaunchReader(arguments))
                }
        }
    }

    sealed class Event {
        class OpenPublicationError(val error: OpeningError) : Event()
        class LaunchReader(val arguments: ReaderActivityContract.Arguments) : Event()
        class LaunchRemoteReader(val book: Book) : Event()
    }
}
