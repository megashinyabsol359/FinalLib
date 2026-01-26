package com.example.finallib.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.finallib.R
import com.example.finallib.model.SellerRequest
import com.example.finallib.model.SystemLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterSellerFragment : Fragment(R.layout.activity_register_seller) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.title = "Đăng ký bán hàng"

        // Tìm view từ biến 'view'
        val btnSend = view.findViewById<Button>(R.id.btn_send_request)

        btnSend.setOnClickListener {
            sendRequest()
        }
    }

    private fun sendRequest() {
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        if (user != null) {
            db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                val name = doc.getString("fullName") ?: "No Name"

                // Tạo request object
                val request = SellerRequest(
                    userId = user.uid,
                    userEmail = user.email ?: "",
                    fullName = name,
                    status = "PENDING"
                )

                db.collection("seller_requests").add(request)
                    .addOnSuccessListener {
                        // Dùng requireContext() để hiện thông báo
                        Toast.makeText(requireContext(), "Đã gửi yêu cầu thành công!", Toast.LENGTH_SHORT).show()

                        // Ghi log
                        val log = SystemLog(user.uid, user.email?:"", name, "User", "REQUEST_SELLER", "Xin làm Seller")
                        db.collection("system_logs").add(log)

                        parentFragmentManager.popBackStack()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}