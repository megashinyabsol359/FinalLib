package com.example.finallib.search

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.Book
import com.example.finallib.model.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class BookDetailActivity : AppCompatActivity() {

    private lateinit var book: Book
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var reviewAdapter: ReviewAdapter
    private val reviewList = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_detail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Nhận dữ liệu từ Intent
        book = intent.getSerializableExtra("book") as Book? ?: Book()

        // Setup toolbar title
        supportActionBar?.title = book.title

        // Setup views
        val tvId: TextView = findViewById(R.id.tv_detail_id)
        val tvTitle: TextView = findViewById(R.id.tv_detail_title)
        val tvAuthor: TextView = findViewById(R.id.tv_detail_author)
        val tvLanguage: TextView = findViewById(R.id.tv_detail_language)
        val tvTags: TextView = findViewById(R.id.tv_detail_tags)
        val btnRead: Button = findViewById(R.id.btn_read)
        val btnRating: Button = findViewById(R.id.btn_rating)
        val rvReviews: RecyclerView = findViewById(R.id.rv_reviews)
        val tvNoReviews: TextView = findViewById(R.id.tv_no_reviews)

        tvId.text = "ID: ${book.id}"
        tvTitle.text = "Tiêu đề: ${book.title}"
        tvAuthor.text = "Tác giả: ${book.author}"
        tvLanguage.text = "Ngôn ngữ: ${book.language}"
        tvTags.text = "Tags: ${book.tags.joinToString(", ")}"

        // Setup RecyclerView Reviews
        reviewAdapter = ReviewAdapter(emptyList())
        rvReviews.layoutManager = LinearLayoutManager(this)
        rvReviews.adapter = reviewAdapter

        btnRead.setOnClickListener {
            Toast.makeText(this, "Chức năng đọc sách sẽ được thêm sau", Toast.LENGTH_SHORT).show()
        }

        btnRating.setOnClickListener {
            showRatingDialog()
        }

        // Fetch reviews
        fetchReviews(rvReviews, tvNoReviews)
    }

    private fun showRatingDialog() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_review, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        val etComment = dialogView.findViewById<EditText>(R.id.et_comment)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_submit_review).setOnClickListener {
            val rating = ratingBar.rating
            val comment = etComment.text.toString().trim()

            if (rating == 0f) {
                Toast.makeText(this, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (comment.isEmpty()) {
                Toast.makeText(this, "Vui lòng viết nhận xét", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitReview(rating, comment, currentUser.uid, currentUser.displayName ?: "Ẩn danh")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun submitReview(rating: Float, comment: String, userId: String, userName: String) {
        val reviewId = UUID.randomUUID().toString()
        val review = Review(
            id = reviewId,
            bookId = book.id,
            userId = userId,
            userName = userName,
            rating = rating,
            comment = comment
        )

        db.collection("reviews")
            .document(reviewId)
            .set(review)
            .addOnSuccessListener {
                Toast.makeText(this, "Đánh giá đã được lưu", Toast.LENGTH_SHORT).show()
                // Refresh reviews list
                fetchReviews(findViewById(R.id.rv_reviews), findViewById(R.id.tv_no_reviews))
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchReviews(rvReviews: RecyclerView, tvNoReviews: TextView) {
        db.collection("reviews")
            .whereEqualTo("bookId", book.id)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                reviewList.clear()
                for (document in documents) {
                    val review = document.toObject(Review::class.java)
                    reviewList.add(review)
                }

                if (reviewList.isNotEmpty()) {
                    reviewAdapter.updateList(reviewList)
                    rvReviews.visibility = RecyclerView.VISIBLE
                    tvNoReviews.visibility = TextView.GONE
                } else {
                    rvReviews.visibility = RecyclerView.GONE
                    tvNoReviews.visibility = TextView.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                tvNoReviews.text = "Lỗi tải đánh giá: ${e.message}"
                rvReviews.visibility = RecyclerView.GONE
                tvNoReviews.visibility = TextView.VISIBLE
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
