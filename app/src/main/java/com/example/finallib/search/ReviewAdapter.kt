package com.example.finallib.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.Review
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewAdapter(private var reviewList: List<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tv_review_username)
        val ratingBar: RatingBar = itemView.findViewById(R.id.rating_bar_review)
        val tvComment: TextView = itemView.findViewById(R.id.tv_review_comment)
        val tvDate: TextView = itemView.findViewById(R.id.tv_review_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviewList[position]
        holder.tvUserName.text = review.userName
        holder.ratingBar.rating = review.rating
        holder.tvComment.text = review.comment
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
        holder.tvDate.text = dateFormat.format(review.timestamp)
    }

    override fun getItemCount(): Int = reviewList.size

    fun updateList(newList: List<Review>) {
        reviewList = newList
        notifyDataSetChanged()
    }
}
