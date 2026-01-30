package com.example.finallib.payment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.finallib.Api.CreateOrder
import com.example.finallib.R
import com.example.finallib.model.Book
import com.example.finallib.model.Purchase
import com.example.finallib.utils.LogUtils // Import LogUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import vn.zalopay.sdk.Environment
import vn.zalopay.sdk.ZaloPayError
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener
import java.text.NumberFormat
import java.util.*

class PaymentActivity : AppCompatActivity() {

    private lateinit var book: Book
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isPaymentProcessing = false // Flag để tránh duplicate processing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        val policy: StrictMode.ThreadPolicy =
            StrictMode.ThreadPolicy.Builder()
                .permitAll()  // Permit tất cả để tránh violation
                .build()
        StrictMode.setThreadPolicy(policy)

        // Khởi tạo ZaloPay (Sandbox)
        ZaloPaySDK.init(553, Environment.SANDBOX)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_payment)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Thanh toán"

        // Nhận dữ liệu sách từ Intent
        book = intent.getSerializableExtra("book") as Book? ?: Book()

        if (book.id.isEmpty()) {
            Toast.makeText(this, "Dữ liệu sách không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
    }

    private fun setupViews() {
        val ivBookCover: ImageView = findViewById(R.id.iv_book_cover_payment)
        val tvBookTitle: TextView = findViewById(R.id.tv_book_title_payment)
        val tvBookAuthor: TextView = findViewById(R.id.tv_book_author_payment)
        val tvPrice: TextView = findViewById(R.id.tv_price_payment)
        val btnPayment: Button = findViewById(R.id.btn_payment)

        // Load cover image
        if (book.cover.isNotEmpty()) {
            Glide.with(this)
                .load(book.cover)
                .centerCrop()
                .into(ivBookCover)
        } else {
            ivBookCover.setImageResource(R.drawable.ic_launcher_foreground)
        }

        // Display info
        tvBookTitle.text = book.title
        tvBookAuthor.text = "Tác giả: ${book.author}"

        val priceFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        tvPrice.text = "Giá: ${priceFormat.format(book.price)}"

        btnPayment.setOnClickListener {
            processPayment()
        }
    }

    private fun processPayment() {
        val orderApi = CreateOrder()
        val priceString = (book.price).toLong().toString()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để mua sách", Toast.LENGTH_SHORT).show()
            return
        }

        // Hiện ProgressBar
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleLarge)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Đang xử lý thanh toán...")
            .setView(progressBar)
            .setCancelable(false)
            .create()
        dialog.show()

        try {
            val data: JSONObject = orderApi.createOrder(priceString)
            val code: String = data.getString("returncode")
            if (code == "1") {
                Log.d("Payment", "CreateOrder Successfully")

                val token: String = data.getString("zptranstoken")

                ZaloPaySDK.getInstance().payOrder(this, token, "demozpdk://app", object : PayOrderListener {

                    // --- THANH TOÁN THÀNH CÔNG ---
                    override fun onPaymentSucceeded(transactionId: String?, transToken: String?, appTransId: String?) {
                        Log.d("Payment", "PayOrder Successfully")
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                // 1. Lưu thông tin Purchase
                                val purchase = Purchase(
                                    id = UUID.randomUUID().toString(),
                                    bookId = book.id,
                                    userId = currentUser.uid,
                                    sellerId = book.sellerId,
                                    purchasedAt = System.currentTimeMillis(),
                                    price = book.price,
                                    paymentMethod = "ZaloPay"
                                )
                                db.collection("purchases").document(purchase.id).set(purchase).await()

                                // 2. (MỚI) Cập nhật doanh thu cho Seller (nếu sách này của Seller đăng)
                                // Giả sử trong object Book có trường 'ownerId' hoặc 'userId' là người đăng sách
                                // Nếu book chưa có trường này, bạn cần thêm vào model Book nhé.
                                // Ở đây mình ví dụ field đó là book.userId (người đăng)
                                if (book.sellerId.isNotEmpty()) {
                                    val sellerRef = db.collection("users").document(book.sellerId)
                                    // Cộng dồn doanh thu bằng FieldValue.increment (An toàn, tránh xung đột)
                                    sellerRef.update("totalRevenue", FieldValue.increment(book.price.toLong()))
                                        .addOnFailureListener { e -> Log.e("Payment", "Lỗi cập nhật doanh thu: ${e.message}") }
                                }

                                // 3. (MỚI) Ghi Log hệ thống
                                withContext(Dispatchers.Main) {
                                    LogUtils.writeLog("PAYMENT_SUCCESS", "Mua sách '${book.title}' giá ${book.price} qua ZaloPay")
                                }

                                // 4. Thông báo và thoát
                                withContext(Dispatchers.Main) {
                                    dialog.dismiss()
                                    Toast.makeText(this@PaymentActivity, "Thanh toán thành công!", Toast.LENGTH_SHORT).show()
                                    finish() // Quay về màn hình trước
                                }

                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    dialog.dismiss()
                                    Toast.makeText(this@PaymentActivity, "Lỗi lưu giao dịch: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                    override fun onPaymentCanceled(zpTransToken: String?, appTransId: String?) {
                        dialog.dismiss()
                        Toast.makeText(this@PaymentActivity, "Đã hủy thanh toán", Toast.LENGTH_SHORT).show()
                    }

                    override fun onPaymentError(error: ZaloPayError?, zpTransToken: String?, appTransId: String?) {
                        dialog.dismiss()
                        Toast.makeText(this@PaymentActivity, "Thanh toán thất bại: ${error.toString()}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                dialog.dismiss()
                Toast.makeText(this, "Lỗi tạo đơn hàng ZaloPay", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            dialog.dismiss()
            Toast.makeText(this, "Lỗi hệ thống: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ZaloPaySDK.getInstance().onResult(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
