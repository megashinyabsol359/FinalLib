package com.example.finallib.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
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

    inner class TagViewHolder(private val checkBox: CheckBox) : RecyclerView.ViewHolder(checkBox) {
        fun bind(tagItem: TagItem) {
            checkBox.text = tagItem.name
            checkBox.isChecked = tagItem.isSelected

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                tagItem.isSelected = isChecked
                onTagSelected(getSelectedTags())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val checkBox = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag_checkbox, parent, false) as CheckBox
        return TagViewHolder(checkBox)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    override fun getItemCount() = tags.size

    fun getSelectedTags(): List<String> = tags.filter { it.isSelected }.map { it.name }

    fun getSelectedTagIds(): List<String> = tags.filter { it.isSelected }.map { it.id }
}
