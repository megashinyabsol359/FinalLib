package com.example.finallib.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.SellerRequest
import com.google.firebase.firestore.FirebaseFirestore

class AdminNotificationFragment : Fragment(R.layout.fragment_system_log) {

    private lateinit var recyclerView: RecyclerView
    private val requestList = ArrayList<SellerRequest>()
    private val docIds = ArrayList<String>()
    private lateinit var adapter: RequestAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.title = "Duyệt yêu cầu bán hàng"

        recyclerView = view.findViewById(R.id.recycler_logs)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = RequestAdapter(requestList, docIds)
        recyclerView.adapter = adapter

        loadRequests()
    }

    private fun loadRequests() {
        val db = FirebaseFirestore.getInstance()

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
            }
    }
}