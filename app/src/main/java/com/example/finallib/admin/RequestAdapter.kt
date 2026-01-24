package com.example.finallib.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.SellerRequest
import com.example.finallib.model.SystemLog
import com.google.firebase.firestore.FirebaseFirestore

class RequestAdapter(
    private val requestList: MutableList<SellerRequest>,
    private val docIds: MutableList<String> // Lưu ID document
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_req_name)
        val tvEmail: TextView = view.findViewById(R.id.tv_req_email)
        val btnApprove: Button = view.findViewById(R.id.btn_approve)
        val btnReject: Button = view.findViewById(R.id.btn_reject) // Nút Bỏ qua mới
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requestList[position]
        holder.tvName.text = request.fullName
        holder.tvEmail.text = request.userEmail

        // Xử lý nút DUYỆT
        holder.btnApprove.setOnClickListener {
            approveUser(request, docIds[position], position, holder.itemView)
        }

        // Xử lý nút BỎ QUA
        holder.btnReject.setOnClickListener {
            rejectRequest(request, docIds[position], position, holder.itemView)
        }
    }

    // Hàm Duyệt
    private fun approveUser(req: SellerRequest, docId: String, position: Int, view: View) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(req.userId)
            .update("role", "Seller")
            .addOnSuccessListener {

                db.collection("seller_requests").document(docId).delete()

                val log = SystemLog("", "Admin", "Admin", "Admin", "APPROVE_SELLER", "Đã duyệt ${req.userEmail}")
                db.collection("system_logs").add(log)

                Toast.makeText(view.context, "Đã duyệt lên Seller!", Toast.LENGTH_SHORT).show()
                removeItem(position)
            }
            .addOnFailureListener {
                Toast.makeText(view.context, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Hàm B
    private fun rejectRequest(req: SellerRequest, docId: String, position: Int, view: View) {
        val db = FirebaseFirestore.getInstance()

        db.collection("seller_requests").document(docId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(view.context, "Đã từ chối yêu cầu!", Toast.LENGTH_SHORT).show()

                val log = SystemLog("", "Admin", "Admin", "Admin", "REJECT_SELLER", "Đã từ chối ${req.userEmail}")
                db.collection("system_logs").add(log)

                removeItem(position)
            }
            .addOnFailureListener {
                Toast.makeText(view.context, "Lỗi xóa: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Xoá item
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