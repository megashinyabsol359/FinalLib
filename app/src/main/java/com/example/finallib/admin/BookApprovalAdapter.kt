package com.example.finallib.admin

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.Book
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class BookApprovalAdapter(
    private val bookList: MutableList<BookApprovalItem>,
    private val activity: BookApprovalActivity
) : RecyclerView.Adapter<BookApprovalAdapter.BookViewHolder>() {

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView = itemView.findViewById(R.id.iv_book_cover)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_book_title)
        val tvAuthor: TextView = itemView.findViewById(R.id.tv_book_author)
        val tvLanguage: TextView = itemView.findViewById(R.id.tv_book_language)
        val tvUploadedBy: TextView = itemView.findViewById(R.id.tv_uploaded_by)
        val tvTags: TextView = itemView.findViewById(R.id.tv_book_tags)
        val btnReview: Button = itemView.findViewById(R.id.btn_review)
        val btnApprove: Button = itemView.findViewById(R.id.btn_approve)
        val btnReject: Button = itemView.findViewById(R.id.btn_reject)
        val llBookContainer: LinearLayout = itemView.findViewById(R.id.ll_book_container)

        private fun approveBook(docId: String, book: Book, activity: BookApprovalActivity) {
            val db = FirebaseFirestore.getInstance()
            db.collection("books").document(docId)
                .update("status", "approved")
                .addOnSuccessListener {
                    Toast.makeText(activity, "✅ Sách đã được duyệt", Toast.LENGTH_SHORT).show()
                    activity.refreshList()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(activity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        private fun rejectBook(docId: String, book: Book, activity: BookApprovalActivity) {
            val db = FirebaseFirestore.getInstance()
            db.collection("books").document(docId)
                .update("status", "rejected")
                .addOnSuccessListener {
                    Toast.makeText(activity, "❌ Sách đã bị từ chối", Toast.LENGTH_SHORT).show()
                    activity.refreshList()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(activity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        fun bind(item: BookApprovalItem, activity: BookApprovalActivity) {
            val book = item.book

            // Load cover
            if (book.cover.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(book.cover)
                    .centerCrop()
                    .into(ivCover)
            } else {
                ivCover.setImageResource(R.drawable.ic_launcher_foreground)
            }

            tvTitle.text = book.title
            tvAuthor.text = "Tác giả: ${book.author}"
            tvLanguage.text = "Ngôn ngữ: ${book.language}"
            tvUploadedBy.text = "Upload bởi: ${book.uploadedBy}"
            tvTags.text = "Tags: ${book.tags.joinToString(", ")}"

            // Review button - Mở DetailActivity nhưng không tính read_count
            btnReview.setOnClickListener {
                val intent = Intent(itemView.context, BookApprovalDetailActivity::class.java)
                intent.putExtra("book", book)
                intent.putExtra("bookDocId", item.docId)
                itemView.context.startActivity(intent)
            }

            // Approve button
            btnApprove.setOnClickListener {
                approveBook(item.docId, book, activity)
            }

            // Reject button
            btnReject.setOnClickListener {
                rejectBook(item.docId, book, activity)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_approval, parent, false)
        return BookViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(bookList[position], activity)
    }

    override fun getItemCount(): Int = bookList.size
}
