package com.example.finallib.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.finallib.R
import com.example.finallib.main.MainActivity
import com.example.finallib.utils.LogUtils // Import LogUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    // Không cần dùng db ở đây nữa vì LogUtils tự xử lý
    
    private lateinit var edtEmail: EditText
    private lateinit var edtPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        edtEmail = findViewById(R.id.edt_email)
        edtPass = findViewById(R.id.edt_password)
        btnLogin = findViewById(R.id.btn_login)
        tvGoToRegister = findViewById(R.id.tv_go_to_register)
        progressBar = findViewById(R.id.progress_bar)

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val pass = edtPass.text.toString().trim()
            if (isValid(email, pass)) {
                loginUser(email, pass)
            }
        }

        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Không cần log ở onStart để tránh spam log mỗi lần mở app
            // Hoặc nếu muốn log thì dùng LogUtils
            // LogUtils.writeLog("AUTO_LOGIN", "Tự động đăng nhập")
            goToMainActivity()
        }
    }

    private fun loginUser(email: String, pass: String) {
        showLoading(true)
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                showLoading(false)
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                // --- GHI LOG MỚI (Dùng LogUtils) ---
                LogUtils.writeLog("LOGIN", "Đăng nhập thành công vào thiết bị")

                goToMainActivity()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Lỗi đăng nhập: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Đã XÓA hàm saveLoginLog() cũ vì không cần thiết nữa

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun isValid(email: String, pass: String): Boolean {
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.isVisible = isLoading
        btnLogin.isEnabled = !isLoading
        tvGoToRegister.isEnabled = !isLoading
    }
}
