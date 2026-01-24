package com.example.finallib.admin // <--- Package chuẩn

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.SystemLog // Import Model
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SystemLogActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val logList = ArrayList<SystemLog>()
    private lateinit var adapter: LogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_log)

        recyclerView = findViewById(R.id.recycler_logs)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = LogAdapter(logList)
        recyclerView.adapter = adapter

        loadLogs()
    }

    private fun loadLogs() {
        val db = FirebaseFirestore.getInstance()

        // Lấy Log
        db.collection("system_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100) // Chỉ lấy 100 dòng cho nhẹ
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
                Toast.makeText(this, "Lỗi tải log: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}