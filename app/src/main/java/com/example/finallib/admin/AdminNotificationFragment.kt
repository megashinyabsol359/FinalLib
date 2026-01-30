package com.example.finallib.admin

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.SellerRequest
import com.google.firebase.firestore.FirebaseFirestore

// Sử dụng lại layout 'fragment_user_list' để tiết kiệm file
class AdminNotificationFragment : Fragment(R.layout.fragment_user_list) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RequestAdapter
    private val requestList = ArrayList<SellerRequest>()
    private val docIds = ArrayList<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.title = "Duyệt yêu cầu bán hàng"

        // 1. ẨN THANH TÌM KIẾM & BỘ LỌC (Vì layout dùng chung nên phải ẩn thủ công)
        val searchView = view.findViewById<View>(R.id.search_view)
        val spinnerRole = view.findViewById<View>(R.id.spinner_role)
        
        // Ẩn view con
        searchView.visibility = View.GONE
        spinnerRole.visibility = View.GONE

        // Mẹo: Ẩn luôn cái CardView bao bên ngoài để giao diện sạch sẽ
        // (Tìm cha của searchView, nếu là CardView thì ẩn nó đi)
        val parentCard = searchView.parent?.parent as? View // Tùy cấu trúc XML
        if (parentCard is CardView) {
            parentCard.visibility = View.GONE
        } else {
            // Nếu không tìm thấy CardView cha, ta ẩn LinearLayout chứa searchView
            (searchView.parent as? View)?.visibility = View.GONE
        }

        // 2. CẤU HÌNH RECYCLERVIEW
        // Lưu ý: ID trong fragment_user_list là 'recycler_users'
        recyclerView = view.findViewById(R.id.recycler_users) 
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = RequestAdapter(requestList, docIds)
        recyclerView.adapter = adapter

        loadRequests()
    }

    private fun loadRequests() {
        val db = FirebaseFirestore.getInstance()
        
        // Chỉ lấy đơn PENDING
        db.collection("seller_requests")
            .whereEqualTo("status", "PENDING")
            .get()
            .addOnSuccessListener { documents ->
                requestList.clear()
                docIds.clear()
                
                for (doc in documents) {
                    val req = doc.toObject(SellerRequest::class.java)
                    requestList.add(req)
                    docIds.add(doc.id)
                }
                adapter.notifyDataSetChanged()

                if (requestList.isEmpty()) {
                    Toast.makeText(requireContext(), "Hiện không có yêu cầu nào", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
