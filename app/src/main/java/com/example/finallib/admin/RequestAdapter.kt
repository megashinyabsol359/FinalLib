package com.example.finallib.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.Seller
import com.example.finallib.model.SellerRequest
import com.example.finallib.utils.LogUtils
import com.google.firebase.firestore.FirebaseFirestore

class RequestAdapter(
    private val requestList: MutableList<SellerRequest>,
    private val docIds: MutableList<String>
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_req_name)
        val tvInfo: TextView = view.findViewById(R.id.tv_req_email) // Đổi ID cho khớp logic
        val btnApprove: Button = view.findViewById(R.id.btn_approve)
        val btnReject: Button = view.findViewById(R.id.btn_reject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val req = requestList[position]

        // 1. Hiển thị thông tin
        holder.tvName.text = "Shop: ${req.shopName}"
        
        // Gộp thông tin chi tiết vào 1 TextView
        val infoText = """
            Người gửi: ${req.shopName}
            Email: ${req.email}
            Zalo: ${req.zaloPhone}
            NH: ${req.bankInfo}
        """.trimIndent()
        holder.tvInfo.text = infoText

        // 2. Sự kiện Duyệt
        holder.btnApprove.setOnClickListener {
            approveRequest(req, docIds[position], position, holder.itemView)
        }

        // 3. Sự kiện Từ chối
        holder.btnReject.setOnClickListener {
            rejectRequest(req, docIds[position], position, holder.itemView)
        }
    }

    private fun approveRequest(req: SellerRequest, docId: String, position: Int, view: View) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        // B1: Cập nhật Role User -> Seller
        val userRef = db.collection("users").document(req.userId)
        batch.update(userRef, "role", "Seller")

        // B2: Tạo hồ sơ trong bảng Sellers
        val sellerRef = db.collection("sellers").document(req.userId)
        val newSeller = Seller(
            userId = req.userId,
            shopName = req.shopName,
            bankInfo = req.bankInfo,
            zaloPhone = req.zaloPhone,
            totalRevenue = 0,
            isActive = true
        )
        batch.set(sellerRef, newSeller)

        // B3: Xóa đơn yêu cầu
        val requestRef = db.collection("seller_requests").document(docId)
        batch.delete(requestRef)

        batch.commit().addOnSuccessListener {
            Toast.makeText(view.context, "Đã duyệt ${req.shopName}!", Toast.LENGTH_SHORT).show()
            LogUtils.writeLog("ADMIN_APPROVE", "Duyệt Shop: ${req.shopName} - User: ${req.email}")
            removeItem(position)
        }.addOnFailureListener {
            Toast.makeText(view.context, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rejectRequest(req: SellerRequest, docId: String, position: Int, view: View) {
        val db = FirebaseFirestore.getInstance()
        
        db.collection("seller_requests").document(docId).delete()
            .addOnSuccessListener {
                Toast.makeText(view.context, "Đã từ chối đơn!", Toast.LENGTH_SHORT).show()
                LogUtils.writeLog("ADMIN_REJECT", "Từ chối Shop: ${req.shopName} - User: ${req.email}")
                removeItem(position)
            }
            .addOnFailureListener {
                Toast.makeText(view.context, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeItem(position: Int) {
        if (position >= 0 && position < requestList.size) {
            requestList.removeAt(position)
            docIds.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, requestList.size)
        }
    }

    override fun getItemCount() = requestList.size
}
