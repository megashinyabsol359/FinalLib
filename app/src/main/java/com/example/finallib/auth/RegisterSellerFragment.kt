package com.example.finallib.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.finallib.R
import com.example.finallib.model.SellerRequest
import com.example.finallib.utils.LogUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class RegisterSellerFragment : Fragment(R.layout.fragment_register_seller) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val edtShopName = view.findViewById<TextInputEditText>(R.id.edt_shop_name)
        val edtZalo = view.findViewById<TextInputEditText>(R.id.edt_zalo)     // Mới
        val edtBank = view.findViewById<TextInputEditText>(R.id.edt_bank)     // Mới
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit_request)

        btnSubmit.setOnClickListener {
            val shopName = edtShopName.text.toString().trim()
            val zalo = edtZalo.text.toString().trim()
            val bank = edtBank.text.toString().trim()

            if (shopName.isEmpty() || zalo.isEmpty() || bank.isEmpty()) {
                Toast.makeText(context, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitRequest(shopName, zalo, bank)
        }
    }

    private fun submitRequest(shopName: String, zalo: String, bank: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // Tạo đối tượng yêu cầu với đầy đủ thông tin
        val request = SellerRequest(
            userId = user.uid,
            email = user.email ?: "",
            shopName = shopName,
            bankInfo = bank,    // Lưu thông tin ngân hàng
            zaloPhone = zalo,   // Lưu Zalo
            status = "PENDING",
            timestamp = Date()
        )

        db.collection("seller_requests")
            .add(request)
            .addOnSuccessListener {
                Toast.makeText(context, "Đã gửi yêu cầu thành công!", Toast.LENGTH_SHORT).show()
                LogUtils.writeLog("REGISTER_SELLER", "Gửi đơn đăng ký shop: $shopName")
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
