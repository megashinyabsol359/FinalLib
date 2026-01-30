package com.example.finallib.search

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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
import com.example.finallib.data.UserBooksRepository
import com.example.finallib.model.Book
import com.example.finallib.model.Purchase
import com.example.finallib.model.Review
import com.example.finallib.model.SystemLog
import com.example.finallib.payment.PaymentActivity
import com.example.finallib.utils.FileDownloadService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.example.finallib.main.Application
import com.example.finallib.reader.ReaderActivityContract
import org.readium.r2.shared.util.toUrl
import org.readium.r2.shared.util.AbsoluteUrl
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class BookDetailActivity : AppCompatActivity() {

    private lateinit var book: Book
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userBooksRepository = UserBooksRepository()
    private lateinit var reviewAdapter: ReviewAdapter
    private val reviewList = mutableListOf<Review>()

    private val app: Application
        get() = application as Application

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
        val ivCover: ImageView = findViewById(R.id.iv_book_cover_detail)
        val tvId: TextView = findViewById(R.id.tv_detail_id)
        val tvTitle: TextView = findViewById(R.id.tv_detail_title)
        val tvAuthor: TextView = findViewById(R.id.tv_detail_author)
        val tvLanguage: TextView = findViewById(R.id.tv_detail_language)
        val tvTags: TextView = findViewById(R.id.tv_detail_tags)
        val btnRead: Button = findViewById(R.id.btn_read)
        val btnRating: Button = findViewById(R.id.btn_rating)
        val rvReviews: RecyclerView = findViewById(R.id.rv_reviews)
        val tvNoReviews: TextView = findViewById(R.id.tv_no_reviews)

        // Load cover image
        if (book.cover.isNotEmpty()) {
            Glide.with(this)
                .load(book.cover)
                .centerCrop()
                .into(ivCover)
        } else {
            ivCover.setImageResource(R.drawable.ic_launcher_foreground) // Fallback image
        }

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
            checkLocalAndDownload()
        }

        btnRating.setOnClickListener {
            showRatingDialog()
        }

        // Fetch reviews
        fetchReviews(rvReviews, tvNoReviews)

        // Check if book is already downloaded
        checkDownloadStatus(btnRead)

        // Check access for private books
        if (book.accessibility == "private") {
            checkAndSetupPrivateBookAccess(btnRead)
        }
    }

    private fun checkDownloadStatus(btnAction: Button) {
        lifecycleScope.launch {
            val localBook = app.bookRepository.books().first().find { 
                it.identifier == book.id || (it.title == book.title && it.author == book.author)
            }
            if (localBook != null) {
                btnAction.text = "Đã tải sách"
                btnAction.isEnabled = false
                btnAction.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            }
        }
    }

    private fun checkLocalAndDownload() {
        lifecycleScope.launch {
            val localBook = app.bookRepository.books().first().find {
                it.identifier == book.id || (it.title == book.title && it.author == book.author)
            }
            if (localBook != null) {
                Toast.makeText(
                    this@BookDetailActivity,
                    "Sách đã có trong tủ sách",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                downloadBook(findViewById(R.id.btn_read))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Kiểm tra lại purchase data khi quay về từ PaymentActivity
        if (book.accessibility == "private") {
            val btnRead: Button = findViewById(R.id.btn_read)
            checkAndSetupPrivateBookAccess(btnRead)
        }
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
        val review = Review(
            bookId = book.id,
            userId = userId,
            userName = userName,
            rating = rating,
            comment = comment
        )

        db.collection("reviews")
            .add(review)
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

    private fun downloadBook(btnAction: Button) {
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
                context = this@BookDetailActivity,
                fileUrl = book.url,
                fileName = fileName
            )

            result.onSuccess { filePath ->
                // If the user is logged in, add the book to their collection
                auth.currentUser?.let { user ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        userBooksRepository.addBookToUser(user.uid, book.id)
                    }
                }
                // Tăng read_count cho sách
                increaseReadCount(book.id)

                // Tạo system log
                createReadingLog()

                // Import book to local library
                val file = File(filePath)
                val url = file.toUrl()
                val importResult = app.bookshelf.addPublication(url as AbsoluteUrl)
                
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    importResult.onSuccess { bookId ->
                        Toast.makeText(this@BookDetailActivity, "Sách tải thành công. Vui lòng vào Tủ sách để đọc.", Toast.LENGTH_LONG).show()
                        btnAction.text = "Đã tải sách"
                        btnAction.isEnabled = false
                        btnAction.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                    }.onFailure {
                        Toast.makeText(this@BookDetailActivity, "Lỗi khi nhập sách vào thư viện: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            result.onFailure { error ->
                lifecycleScope.launch(Dispatchers.Main) {
                    dialog.dismiss()
                    Toast.makeText(
                        this@BookDetailActivity,
                        "Lỗi tải sách: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun checkAndSetupPrivateBookAccess(btnRead: Button) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in, show purchase button
            setupPurchaseButton(btnRead)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val querySnapshot = db.collection("purchases")
                    .whereEqualTo("bookId", book.id)
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()

                lifecycleScope.launch(Dispatchers.Main) {
                    if (querySnapshot.isEmpty) {
                        // User hasn't purchased, show purchase button
                        setupPurchaseButton(btnRead)
                    } else {
                        // User has purchased, keep download action
                        btnRead.text = "Tải sách"
                        btnRead.setBackgroundColor(resources.getColor(android.R.color.holo_purple))
                        btnRead.setOnClickListener {
                            checkLocalAndDownload()
                        }
                        // Re-check if already downloaded to disable
                        checkDownloadStatus(btnRead)
                    }
                }
            } catch (e: Exception) {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@BookDetailActivity,
                        "Lỗi kiểm tra quyền truy cập: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    setupPurchaseButton(btnRead)
                }
            }
        }
    }

    private fun setupPurchaseButton(btnRead: Button) {
        btnRead.text = "Mua sách"
        btnRead.setBackgroundColor(resources.getColor(android.R.color.holo_orange_light))
        btnRead.setOnClickListener {
            purchaseBook(btnRead)
        }
    }

    private fun purchaseBook(btnRead: Button) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để mua sách", Toast.LENGTH_SHORT).show()
            return
        }

        // Chuyển sang màn hình thanh toán
        goToPayment()
    }

    /**
     * Chuyển sang màn hình thanh toán
     */
    private fun goToPayment() {
        val intent = Intent(this, PaymentActivity::class.java)
        intent.putExtra("book", book)
        startActivity(intent)
    }

    /**
     * Tăng read_count cho sách
     */
    private fun increaseReadCount(bookId: String) {
        db.collection("books").document(bookId)
            .update("readCount", com.google.firebase.firestore.FieldValue.increment(1))
            .addOnSuccessListener {
                // Thành công
            }
            .addOnFailureListener { e ->
                // Có thể log lỗi nếu cần
            }
    }

    /**
     * Lấy role người dùng từ Firestore
     */
    private fun getUserRole(userId: String, callback: (String) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role") ?: "reader"
                callback(role)
            }
            .addOnFailureListener {
                callback("reader")  // Default role
            }
    }

    /**
     * Tạo system log khi người dùng đọc sách
     */
    private fun createReadingLog() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        try {
            // Lấy role từ Firestore
            getUserRole(currentUser.uid) { role ->
                val systemLog = SystemLog(
                    userId = currentUser.uid,
                    email = currentUser.email ?: "",
                    fullName = currentUser.displayName ?: "Ẩn danh",
                    role = role,
                    action = "READING",
                    details = "Đọc sách ${book.title} (${book.id})"
                )

                db.collection("system_logs")
                    .add(systemLog)
                    .addOnSuccessListener {
                        // Log thành công
                    }
                    .addOnFailureListener { e ->
                        // Log thất bại (không cần notify user)
                    }
            }
        } catch (e: Exception) {
            // Handle exception silently
        }
    }

}
