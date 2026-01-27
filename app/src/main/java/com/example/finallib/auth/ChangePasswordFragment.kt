package com.example.finallib.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.finallib.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordFragment : Fragment(R.layout.activity_change_password) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.title = "Đổi mật khẩu"

        val edtOldPass = view.findViewById<EditText>(R.id.edt_old_pass)
        val edtNewPass = view.findViewById<EditText>(R.id.edt_new_pass)
        val edtConfirmPass = view.findViewById<EditText>(R.id.edt_confirm_pass)
        val btnChange = view.findViewById<Button>(R.id.btn_change_pass)

        btnChange.setOnClickListener {
            val oldPass = edtOldPass.text.toString().trim()
            val newPass = edtNewPass.text.toString().trim()
            val confirmPass = edtConfirmPass.text.toString().trim()

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(requireContext(), "Mật khẩu mới phải trên 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(requireContext(), "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()

                            parentFragmentManager.popBackStack()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Lỗi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Mật khẩu cũ không đúng!", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Lỗi: Chưa đăng nhập!", Toast.LENGTH_SHORT).show()
        }
    }
}