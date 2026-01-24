package com.example.finallib.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.finallib.R
import com.example.finallib.main.MainActivity
import com.example.finallib.model.SystemLog // Import Model SystemLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var edtEmail: EditText
    private lateinit var edtPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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
            // Log
            saveLoginLog(currentUser.uid, currentUser.email ?: "Unknown")
            goToMainActivity()
        }
    }

    private fun loginUser(email: String, pass: String) {
        showLoading(true)
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                showLoading(false)
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                val user = result.user
                if (user != null) {
                    // Log
                    saveLoginLog(user.uid, email)
                }

                goToMainActivity()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Lỗi đăng nhập: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Hàm ghi Log
    private fun saveLoginLog(userId: String, email: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val currentRole = document.getString("role") ?: "User"
                val currentName = document.getString("fullName") ?: "Unknown"


                val log = SystemLog(
                    userId = userId,
                    email = email,
                    fullName = currentName,
                    role = currentRole,
                    action = "LOGIN",
                    details = "Đăng nhập hệ thống ($currentRole)"
                )

                db.collection("system_logs").add(log)
                    .addOnSuccessListener {
                        Log.d("SystemLog", "Đã lưu log login cho: $currentName ($currentRole)")
                    }
            }
            .addOnFailureListener {
                val log = SystemLog(userId, email, "Unknown", "Unknown", "LOGIN", "Lỗi lấy thông tin user")
                db.collection("system_logs").add(log)
            }
    }

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