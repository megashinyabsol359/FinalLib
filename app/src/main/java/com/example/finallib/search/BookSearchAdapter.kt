package com.example.finallib.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.Book
import android.content.Intent
import com.bumptech.glide.Glide
import android.graphics.drawable.GradientDrawable

class BookSearchAdapter(private var bookList: List<Book>) : RecyclerView.Adapter<BookSearchAdapter.BookViewHolder>() {

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView = itemView.findViewById(R.id.iv_book_cover)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_book_title)
        val tvAuthor: TextView = itemView.findViewById(R.id.tv_book_author)
        val tvLanguage: TextView = itemView.findViewById(R.id.tv_book_language)
        val tvTags: TextView = itemView.findViewById(R.id.tv_book_tags)
        val llBookContainer: LinearLayout = itemView.findViewById(R.id.ll_book_container)
        val tvPrivateBadge: TextView = itemView.findViewById(R.id.tv_private_badge)

        fun bind(book: Book) {
            // Load cover image
            if (book.cover.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(book.cover)
                    .centerCrop()
                    .into(ivCover)
            } else {
                ivCover.setImageResource(R.drawable.ic_launcher_foreground) // Fallback image
            }

            tvTitle.text = book.title
            tvAuthor.text = "Tác giả: ${book.author}"
            tvLanguage.text = "Ngôn ngữ: ${book.language}"
            tvTags.text = "Tags: ${book.tags.joinToString(", ")}"

            // Apply yellow border if book is private
            if (book.accessibility == "private") {
                val border = GradientDrawable()
                border.setColor(itemView.context.resources.getColor(android.R.color.white))
                border.setStroke(4, itemView.context.resources.getColor(android.R.color.holo_orange_light))
                llBookContainer.background = border
                tvPrivateBadge.visibility = View.VISIBLE
            } else {
                tvPrivateBadge.visibility = View.GONE
                llBookContainer.setBackgroundResource(R.drawable.item_book_background)
            }

            // Click listener để navigate to BookDetailActivity
            itemView.setOnClickListener {
                val intent = Intent(itemView.context, BookDetailActivity::class.java)
                intent.putExtra("book", book)
                itemView.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_search, parent, false)
        return BookViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = bookList[position]
        holder.bind(book)
    }

    override fun getItemCount(): Int = bookList.size

    fun updateList(newList: List<Book>) {
        bookList = newList
        notifyDataSetChanged()
    }
}
