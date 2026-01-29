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
    private var userList: List<User>,
    private val currentUserId: String,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {


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

        holder.tvRole.text = "Vai trò: ${user.role}"
        when (user.role) {
            "Admin" -> holder.tvRole.setTextColor(Color.RED)
            "Seller" -> holder.tvRole.setTextColor(Color.BLUE)
            else -> holder.tvRole.setTextColor(Color.GRAY)
        }

        if (user.id == currentUserId) {
            holder.tvName.text = "${user.fullName} (Bạn)"
            holder.tvName.setTextColor(Color.parseColor("#009688"))
            holder.tvName.setTypeface(null, Typeface.BOLD) // In đậm
        } else {
            holder.tvName.text = user.fullName
            holder.tvName.setTextColor(Color.BLACK)
            holder.tvName.setTypeface(null, Typeface.NORMAL)
        }

        holder.itemView.setOnClickListener {
            onItemClick(user)
        }
    }

    override fun getItemCount() = userList.size

    fun updateList(newList: List<User>) {
        userList = newList
        notifyDataSetChanged()
    }
}