package com.example.finallib.admin

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.Book
import com.google.firebase.firestore.FirebaseFirestore

class BookApprovalActivity : AppCompatActivity() {

    private lateinit var rvPendingBooks: RecyclerView
    private lateinit var tvNoBooks: TextView
    private lateinit var progressBar: ProgressBar
    private val db = FirebaseFirestore.getInstance()
    private val pendingBooks = mutableListOf<BookApprovalItem>()
    private lateinit var bookApprovalAdapter: BookApprovalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_approval)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_approval)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Duyệt Sách Upload")

        rvPendingBooks = findViewById(R.id.rv_pending_books)
        tvNoBooks = findViewById(R.id.tv_no_books)
        progressBar = findViewById(R.id.progress_bar)

        // Setup RecyclerView
        bookApprovalAdapter = BookApprovalAdapter(pendingBooks, this)
        rvPendingBooks.layoutManager = LinearLayoutManager(this)
        rvPendingBooks.adapter = bookApprovalAdapter

        // Load pending books
        loadPendingBooks()
    }

    private fun loadPendingBooks() {
        progressBar.visibility = ProgressBar.VISIBLE
        tvNoBooks.visibility = TextView.GONE

        db.collection("books")
            .get()
            .addOnSuccessListener { snapshot ->
                pendingBooks.clear()
                for (document in snapshot.documents) {
                    val book = document.toObject(Book::class.java)
                    // Lọc sách có status = "pending"
                    if (book != null && book.status == "pending") {
                        pendingBooks.add(BookApprovalItem(document.id, book))
                    }
                }

                // Sắp xếp theo uploadedAt giảm dần (mới nhất trước)
                pendingBooks.sortByDescending { it.book.uploadedAt }

                progressBar.visibility = ProgressBar.GONE

                if (pendingBooks.isEmpty()) {
                    tvNoBooks.text = "Không có sách chờ duyệt"
                    tvNoBooks.visibility = TextView.VISIBLE
                    rvPendingBooks.visibility = RecyclerView.GONE
                } else {
                    tvNoBooks.visibility = TextView.GONE
                    rvPendingBooks.visibility = RecyclerView.VISIBLE
                    bookApprovalAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = ProgressBar.GONE
                tvNoBooks.text = "Lỗi tải sách: ${e.message}"
                tvNoBooks.visibility = TextView.VISIBLE
            }
    }

    fun refreshList() {
        loadPendingBooks()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

data class BookApprovalItem(
    val docId: String,
    val book: Book
)
