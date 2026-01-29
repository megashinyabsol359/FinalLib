package com.example.finallib.payment

import android.app.AlertDialog
import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.finallib.R
import com.example.finallib.model.Book
import com.example.finallib.model.Purchase
import com.bumptech.glide.Glide
import com.example.finallib.Api.CreateOrder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        val policy: StrictMode.ThreadPolicy =
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

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

        // Setup views
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

        // Display book information
        tvBookTitle.text = book.title
        tvBookAuthor.text = "Tác giả: ${book.author}"

        // Format price with Vietnamese locale
        val priceFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        tvPrice.text = "Giá: ${priceFormat.format(book.price)}"

        // Payment button listener
        btnPayment.setOnClickListener {
            processPayment()
        }
    }

    /**
     * Xử lý thanh toán và lưu purchase data lên Firebase
     */
    private fun processPayment() {
        val orderApi: CreateOrder = CreateOrder()
        val priceString = (book.price).toLong().toString()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Lỗi: Người dùng không xác định", Toast.LENGTH_SHORT).show()
            return
        }

        // Hiện ProgressBar
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleLarge)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Đang xử lý thanh toán...")
            .setView(progressBar)
            .setCancelable(false)
            .show()

        try {
            Log.d("priceString", priceString)
            val data: JSONObject = orderApi.createOrder(priceString)
            Log.d("data", data.toString())
            val code: String = data.getString("returncode")
            if (code.equals("1")) {
                val token: String = data.getString("zptranstoken")
                ZaloPaySDK.getInstance().payOrder(this, token, "demozpdk://app", object : PayOrderListener {
                    override fun onPaymentSucceeded(transactionId: String?, transToken: String?, appTransId: String?) {
                        // Thanh toán thành công, lưu thông tin purchase lên Firebase
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                // Tạo object Purchase
                                val purchase = Purchase(
                                    id = UUID.randomUUID().toString(),
                                    bookId = book.id,
                                    userId = currentUser.uid,
                                    purchasedAt = System.currentTimeMillis(),
                                    price = book.price,
                                    paymentMethod = "ZaloPay"
                                )

                                // Lưu lên Firebase
                                db.collection("purchases")
                                    .document(purchase.id)
                                    .set(purchase)
                                    .await()

                                lifecycleScope.launch(Dispatchers.Main) {
                                    dialog.dismiss()
                                    Toast.makeText(
                                        this@PaymentActivity,
                                        "Thanh toán thành công!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Quay lại màn hình trước
                                    finish()
                                }

                            } catch (e: Exception) {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    dialog.dismiss()
                                    Toast.makeText(
                                        this@PaymentActivity,
                                        "Lỗi: ${e.message}. Vui lòng chụp lại hóa đơn để làm thủ tục khiếu nại.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }

                    override fun onPaymentCanceled(zpTransToken: String?, appTransId: String?) {
                        Toast.makeText(this@PaymentActivity, "Thanh toán bị hủy", Toast.LENGTH_SHORT).show()
                    }

                    override fun onPaymentError(error: ZaloPayError?, zpTransToken: String?, appTransId: String?) {
                        Toast.makeText(this@PaymentActivity, "Thanh toán thất bại", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
        catch (e: Exception) {
            dialog.dismiss()
            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ZaloPaySDK.getInstance().onResult(intent)
    }
}
