package com.example.finallib.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.SystemLog
import java.text.SimpleDateFormat
import java.util.Locale

class LogAdapter(private val logList: List<SystemLog>) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAction: TextView = itemView.findViewById(R.id.tv_action)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val tvUserInfo: TextView = itemView.findViewById(R.id.tv_user_info)
        val tvDetails: TextView = itemView.findViewById(R.id.tv_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logList[position]

        // 1. Hiển thị Action và Đổi màu để dễ phân biệt
        holder.tvAction.text = log.action
        
        // Logic tô màu đơn giản:
        when {
            // Màu ĐỎ cho các hành động tiêu cực/nguy hiểm
            log.action.contains("REJECT") || log.action.contains("DELETE") || log.action.contains("ERROR") -> {
                holder.tvAction.setTextColor(Color.RED)
            }
            // Màu XANH LÁ cho các hành động tích cực/thành công
            log.action.contains("APPROVE") || log.action.contains("SUCCESS") || log.action.contains("PAYMENT") -> {
                holder.tvAction.setTextColor(Color.parseColor("#4CAF50")) 
            }
            // Màu ĐEN cho các hành động bình thường
            else -> {
                holder.tvAction.setTextColor(Color.BLACK)
            }
        }

        // 2. Hiển thị User Info thông minh hơn
        // Nếu không có tên (do LogUtils không lấy được), thì chỉ hiện Email cho đỡ rối
        if (log.fullName.isNotEmpty() && log.fullName != "Unknown") {
            holder.tvUserInfo.text = "${log.fullName} (${log.email})"
        } else {
            holder.tvUserInfo.text = log.email // Chỉ hiện email
        }

        // 3. Hiển thị chi tiết
        holder.tvDetails.text = log.details

        // 4. Format thời gian
        if (log.timestamp != null) {
            val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
            holder.tvTime.text = sdf.format(log.timestamp)
        } else {
            holder.tvTime.text = "Vừa xong"
        }
    }

    override fun getItemCount() = logList.size
}
