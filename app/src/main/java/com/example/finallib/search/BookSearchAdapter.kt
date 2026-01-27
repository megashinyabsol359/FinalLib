package com.example.finallib.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.Book
import android.content.Intent

class BookSearchAdapter(private var bookList: List<Book>) : RecyclerView.Adapter<BookSearchAdapter.BookViewHolder>() {

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_book_title)
        val tvAuthor: TextView = itemView.findViewById(R.id.tv_book_author)
        val tvLanguage: TextView = itemView.findViewById(R.id.tv_book_language)
        val tvTags: TextView = itemView.findViewById(R.id.tv_book_tags)

        fun bind(book: Book) {
            tvTitle.text = book.title
            tvAuthor.text = "Tác giả: ${book.author}"
            tvLanguage.text = "Ngôn ngữ: ${book.language}"
            tvTags.text = "Tags: ${book.tags.joinToString(", ")}"

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
