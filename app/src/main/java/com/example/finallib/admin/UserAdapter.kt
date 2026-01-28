package com.example.finallib.admin

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.User

class UserAdapter(
    private var userList: List<User>, // Dùng 'var' để có thể gán danh sách mới khi lọc
    private val currentUserId: String, // ID người đang đăng nhập
    private val onItemClick: (User) -> Unit // Callback khi bấm vào dòng
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    // ViewHolder: Ánh xạ các View trong layout item_user.xml
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_user_name)
        val tvRole: TextView = view.findViewById(R.id.tv_user_role)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        // --- 1. XỬ LÝ MÀU SẮC CHO VAI TRÒ (ROLE) ---
        holder.tvRole.text = "Vai trò: ${user.role}"
        when (user.role) {
            "Admin" -> holder.tvRole.setTextColor(Color.RED)
            "Seller" -> holder.tvRole.setTextColor(Color.BLUE)
            else -> holder.tvRole.setTextColor(Color.GRAY)
        }

        // --- 2. XỬ LÝ HIGHLIGHT "CHÍNH CHỦ" ---
        if (user.id == currentUserId) {
            // Nếu là tài khoản đang đăng nhập
            holder.tvName.text = "${user.fullName} (Bạn)"
            holder.tvName.setTextColor(Color.parseColor("#009688")) // Màu xanh Teal
            holder.tvName.setTypeface(null, Typeface.BOLD) // In đậm
        } else {
            // Nếu là người khác
            holder.tvName.text = user.fullName
            holder.tvName.setTextColor(Color.BLACK)
            holder.tvName.setTypeface(null, Typeface.NORMAL)
        }

        // --- 3. BẮT SỰ KIỆN CLICK ---
        holder.itemView.setOnClickListener {
            onItemClick(user)
        }
    }

    override fun getItemCount() = userList.size

    // --- 4. HÀM CẬP NHẬT DANH SÁCH (Dùng cho Tìm kiếm & Lọc) ---
    fun updateList(newList: List<User>) {
        userList = newList
        notifyDataSetChanged() // Làm mới giao diện RecyclerView
    }
}