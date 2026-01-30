package com.example.finallib.admin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.Book
import com.example.finallib.model.Review
import com.example.finallib.search.ReviewAdapter
import com.example.finallib.search.BookReaderActivity
import com.example.finallib.utils.FileDownloadService
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat

class BookApprovalDetailActivity : AppCompatActivity() {

    private lateinit var book: Book
    private var bookDocId: String = ""
    private val db = FirebaseFirestore.getInstance()
    private lateinit var reviewAdapter: ReviewAdapter
    private val reviewList = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_approval_detail)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_approval_detail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Nhận dữ liệu từ Intent
        book = intent.getSerializableExtra("book") as Book? ?: Book()
        bookDocId = intent.getStringExtra("bookDocId") ?: ""

        displayBookDetails()
        setupReviewsList()
        loadReviews()
    }

    private fun displayBookDetails() {
        val ivCover = findViewById<ImageView>(R.id.iv_book_cover)
        val tvTitle = findViewById<TextView>(R.id.tv_title)
        val tvAuthor = findViewById<TextView>(R.id.tv_author)
        val tvDescription = findViewById<TextView>(R.id.tv_description)
        val tvLanguage = findViewById<TextView>(R.id.tv_language)
        val tvAccessibility = findViewById<TextView>(R.id.tv_accessibility)
        val tvPrice = findViewById<TextView>(R.id.tv_price)
        val tvTags = findViewById<TextView>(R.id.tv_tags)
        val tvStatus = findViewById<TextView>(R.id.tv_status)
        val tvUploadedBy = findViewById<TextView>(R.id.tv_uploaded_by)
        val btnRead = findViewById<Button>(R.id.btn_read)
        val btnApprove = findViewById<Button>(R.id.btn_approve_detail)
        val btnReject = findViewById<Button>(R.id.btn_reject_detail)

        // Load cover
        if (book.cover.isNotEmpty()) {
            Glide.with(this)
                .load(book.cover)
                .centerCrop()
                .into(ivCover)
        }

        tvTitle.text = book.title
        tvAuthor.text = "Tác giả: ${book.author}"
        tvDescription.text = book.description
        tvLanguage.text = "Ngôn ngữ: ${book.language}"
        tvAccessibility.text = "Loại sách: ${if (book.accessibility == "public") "Miễn phí" else "Trả phí"}"

        if (book.price > 0) {
            val priceFormat = DecimalFormat("#,###")
            tvPrice.text = "Giá: ${priceFormat.format(book.price)} VND"
        } else {
            tvPrice.text = "Giá: Miễn phí"
        }

        tvTags.text = "Tags: ${book.tags.joinToString(", ")}"
        tvStatus.text = "Trạng thái: ${book.status}"
        tvUploadedBy.text = "Upload bởi: ${book.uploadedBy}"

        // Read button - KHÔNG tính read_count, KHÔNG tạo system logs
        btnRead.setOnClickListener {
            downloadAndReadBook()
        }

        // Approve button
        btnApprove.setOnClickListener {
            updateBookStatus("approved")
        }

        // Reject button
        btnReject.setOnClickListener {
            updateBookStatus("rejected")
        }
    }

    private fun downloadAndReadBook() {
        if (book.url.isEmpty()) {
            Toast.makeText(this, "Sách này không có file để tải", Toast.LENGTH_SHORT).show()
            return
        }

        // Hiện ProgressBar
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleLarge)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Đang tải sách...")
            .setView(progressBar)
            .setCancelable(false)
            .show()

        // Download file
        lifecycleScope.launch(Dispatchers.IO) {
            val fileName = "${book.id}_${book.title.replace(" ", "_")}.epub"
            
            val result = FileDownloadService.downloadFile(
                context = this@BookApprovalDetailActivity,
                fileUrl = book.url,
                fileName = fileName
            )

            result.onSuccess { filePath ->
                // KHÔNG tăng read_count cho admin review
                // KHÔNG tạo system log cho admin review

                // Chuyển sang BookReaderActivity
                val intent = Intent(this@BookApprovalDetailActivity, BookReaderActivity::class.java)
                intent.putExtra("bookTitle", book.title)
                intent.putExtra("tempFilePath", filePath)
                startActivity(intent)

                // Đóng dialog
                lifecycleScope.launch(Dispatchers.Main) {
                    dialog.dismiss()
                    Toast.makeText(
                        this@BookApprovalDetailActivity,
                        "Sách tải thành công",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            result.onFailure { error ->
                lifecycleScope.launch(Dispatchers.Main) {
                    dialog.dismiss()
                    Toast.makeText(
                        this@BookApprovalDetailActivity,
                        "Lỗi download: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupReviewsList() {
        val rvReviews = findViewById<RecyclerView>(R.id.rv_reviews)
        rvReviews.layoutManager = LinearLayoutManager(this)
        reviewAdapter = ReviewAdapter(reviewList)
        rvReviews.adapter = reviewAdapter
    }

    private fun loadReviews() {
        db.collection("reviews")
            .whereEqualTo("bookId", book.id)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                reviewList.clear()
                for (document in snapshot.documents) {
                    val review = document.toObject(Review::class.java)
                    if (review != null) {
                        reviewList.add(review)
                    }
                }
                reviewAdapter.notifyDataSetChanged()

                val tvNoReviews = findViewById<TextView>(R.id.tv_no_reviews)
                tvNoReviews.visibility = if (reviewList.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
    }

    private fun updateBookStatus(newStatus: String) {
        db.collection("books").document(bookDocId)
            .update("status", newStatus)
            .addOnSuccessListener {
                val message = if (newStatus == "approved") "✅ Sách đã được duyệt" else "❌ Sách đã bị từ chối"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
