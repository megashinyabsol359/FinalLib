package com.example.finallib.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.SystemLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SystemLogFragment : Fragment(R.layout.activity_system_log) {

    private lateinit var recyclerView: RecyclerView
    private val logList = ArrayList<SystemLog>()
    private lateinit var adapter: LogAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_logs)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = LogAdapter(logList)
        recyclerView.adapter = adapter

        loadLogs()
    }

    private fun loadLogs() {
        val db = FirebaseFirestore.getInstance()

        db.collection("system_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { documents ->
                logList.clear()
                for (document in documents) {
                    val log = document.toObject(SystemLog::class.java)
                    logList.add(log)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi tải log: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}