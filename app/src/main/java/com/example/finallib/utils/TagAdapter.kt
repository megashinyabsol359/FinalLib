package com.example.finallib.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R

data class TagItem(
    val id: String,
    val name: String,
    var isSelected: Boolean = false
)

class TagAdapter(
    private val tags: List<TagItem>,
    private val onTagSelected: (List<String>) -> Unit
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    inner class TagViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTagName: TextView = view.findViewById(R.id.tv_tag_name)

        fun bind(tagItem: TagItem) {
            tvTagName.text = tagItem.name
            tvTagName.isSelected = tagItem.isSelected

            tvTagName.setOnClickListener {
                tagItem.isSelected = !tagItem.isSelected
                tvTagName.isSelected = tagItem.isSelected
                onTagSelected(getSelectedTags())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag_checkbox, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    override fun getItemCount() = tags.size

    fun getSelectedTags(): List<String> = tags.filter { it.isSelected }.map { it.name }

    fun getSelectedTagIds(): List<String> = tags.filter { it.isSelected }.map { it.id }
}