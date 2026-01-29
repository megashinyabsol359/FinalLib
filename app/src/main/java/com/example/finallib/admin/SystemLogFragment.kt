package com.example.finallib.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.SystemLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SystemLogFragment : Fragment(R.layout.fragment_system_log) {

    private lateinit var recyclerView: RecyclerView
    private val logList = ArrayList<SystemLog>()
    private lateinit var adapter: LogAdapter

    private var targetUserId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Nhận dữ liệu ID từ UserListFragment gửi sang (nếu có)
        targetUserId = arguments?.getString("USER_ID")

        val title = if (targetUserId != null) "Nhật ký người dùng" else "Nhật ký hệ thống"
        (activity as? AppCompatActivity)?.supportActionBar?.title = title

        recyclerView = view.findViewById(R.id.recycler_logs)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = LogAdapter(logList)
        recyclerView.adapter = adapter

        loadLogs()
    }

    private fun loadLogs() {
        val db = FirebaseFirestore.getInstance()
        var query: Query = db.collection("system_logs")

        // Logic lọc:
        if (targetUserId != null) {
            // Nếu có ID -> Chỉ lấy log của ID đó
            query = query.whereEqualTo("userId", targetUserId)
        }

        // Sắp xếp thời gian giảm dần
        query = query.orderBy("timestamp", Query.Direction.DESCENDING).limit(100)

        query.get()
            .addOnSuccessListener { documents ->
                logList.clear()
                for (document in documents) {
                    val log = document.toObject(SystemLog::class.java)
                    logList.add(log)
                }
                adapter.notifyDataSetChanged()

                if (logList.isEmpty()) {
                    Toast.makeText(requireContext(), "Người này chưa có hoạt động nào", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi tải log: ${it.message}", Toast.LENGTH_SHORT).show()
                // Nếu lỗi yêu cầu Index, hãy xem Logcat để lấy link tạo Index trên Firebase
            }
    }
}