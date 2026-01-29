package com.example.finallib.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.finallib.R
import com.example.finallib.model.SystemLog // Import Model SystemLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPass: EditText
    private lateinit var edtConfirmPass: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        edtName = findViewById(R.id.edt_name)
        edtEmail = findViewById(R.id.edt_email)
        edtPass = findViewById(R.id.edt_password)
        edtConfirmPass = findViewById(R.id.edt_confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        tvGoToLogin = findViewById(R.id.tv_go_to_login)
        progressBar = findViewById(R.id.progress_bar)

        btnRegister.setOnClickListener {
            val name = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val pass = edtPass.text.toString().trim()
            val confirmPass = edtConfirmPass.text.toString().trim()

            if (validateInput(name, email, pass, confirmPass)) {
                registerUser(name, email, pass)
            }
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser(name: String, email: String, pass: String) {
        showLoading(true)

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                if (userId != null) {
                    saveUserToFirestore(userId, name, email)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Lỗi đăng ký: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserToFirestore(userId: String, name: String, email: String) {
        val userMap = hashMapOf(
            "fullName" to name,
            "email" to email,
            "role" to "User",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(userMap)
            .addOnSuccessListener {
                showLoading(false)

                // Log
                val log = SystemLog(
                    userId = userId,
                    email = email,
                    fullName = name,
                    role = "User",
                    action = "REGISTER",
                    details = "Đăng ký tài khoản mới"
                )

                db.collection("system_logs").add(log)

                Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show()

                auth.signOut()

                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Lỗi lưu dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInput(name: String, email: String, pass: String, confirmPass: String): Boolean {
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
            return false
        }
        if (pass.length < 6) {
            Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show()
            return false
        }
        if (pass != confirmPass) { // Kiểm tra mật khẩu nhập lại
            Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.isVisible = isLoading
        btnRegister.isEnabled = !isLoading
    }
}