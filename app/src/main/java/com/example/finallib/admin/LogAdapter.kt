package com.example.finallib.admin // <--- Package chuẩn

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.SystemLog // Import từ package model
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
        // File layout item_log.xml
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logList[position]

        holder.tvAction.text = log.action
        holder.tvUserInfo.text = "${log.fullName} (${log.email})" // Hiện Tên + Email
        holder.tvDetails.text = log.details

        if (log.timestamp != null) {
            val sdf = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
            holder.tvTime.text = sdf.format(log.timestamp)
        } else {
            holder.tvTime.text = "..."
        }
    }

    override fun getItemCount() = logList.size
}