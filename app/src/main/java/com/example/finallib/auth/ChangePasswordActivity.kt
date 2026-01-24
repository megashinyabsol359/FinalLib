package com.example.finallib.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finallib.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val edtOldPass = findViewById<EditText>(R.id.edt_old_pass)
        val edtNewPass = findViewById<EditText>(R.id.edt_new_pass)
        val edtConfirmPass = findViewById<EditText>(R.id.edt_confirm_pass)
        val btnChange = findViewById<Button>(R.id.btn_change_pass)

        btnChange.setOnClickListener {
            val oldPass = edtOldPass.text.toString()
            val newPass = edtNewPass.text.toString()
            val confirmPass = edtConfirmPass.text.toString()

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(this, "Mật khẩu mới phải trên 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            changePassword(oldPass, newPass)
        }
    }

    private fun changePassword(oldPass: String, newPass: String) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPass)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                            finish() // Đóng màn hình
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Lỗi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Mật khẩu cũ không đúng!", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Lỗi: Chưa đăng nhập!", Toast.LENGTH_SHORT).show()
        }
    }
}