package com.example.finallib.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.SellerRequest
import com.google.firebase.firestore.FirebaseFirestore

class AdminNotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val requestList = ArrayList<SellerRequest>()
    private val docIds = ArrayList<String>() // List chứa ID của document để xử lý xóa
    private lateinit var adapter: RequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_log)

        supportActionBar?.title = "Duyệt yêu cầu bán hàng"

        recyclerView = findViewById(R.id.recycler_logs)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = RequestAdapter(requestList, docIds)
        recyclerView.adapter = adapter

        loadRequests()
    }

    private fun loadRequests() {
        val db = FirebaseFirestore.getInstance()

        // Chỉ lấy những đơn có status = PENDING
        db.collection("seller_requests")
            .whereEqualTo("status", "PENDING")
            .get()
            .addOnSuccessListener { documents ->
                requestList.clear()
                docIds.clear()
                for (doc in documents) {
                    val req = doc.toObject(SellerRequest::class.java)
                    requestList.add(req)
                    docIds.add(doc.id) // Lưu ID document
                }
                adapter.notifyDataSetChanged()
            }
    }
}