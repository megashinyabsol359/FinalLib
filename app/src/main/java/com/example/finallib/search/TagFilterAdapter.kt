package com.example.finallib.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.Tag

data class TagFilterItem(
    val tag: Tag,
    var state: Int = 0  // 0: Normal, 1: Include (Green), 2: Exclude (Red)
)

class TagFilterAdapter(
    private val tagList: List<TagFilterItem>,
    private val onTagFilterChanged: () -> Unit
) : RecyclerView.Adapter<TagFilterAdapter.TagFilterViewHolder>() {

    class TagFilterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTag: TextView = view.findViewById(R.id.tv_tag_filter_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagFilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag_filter, parent, false)
        return TagFilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagFilterViewHolder, position: Int) {
        val tagItem = tagList[position]
        holder.tvTag.text = tagItem.tag.label
        updateButtonState(holder.tvTag, tagItem)

        holder.tvTag.setOnClickListener {
            tagItem.state = (tagItem.state + 1) % 3
            updateButtonState(holder.tvTag, tagItem)
            onTagFilterChanged()
        }
    }

    private fun updateButtonState(textView: TextView, tagItem: TagFilterItem) {
        when (tagItem.state) {
            0 -> {
                textView.setBackgroundResource(R.drawable.tag_button_normal)
                textView.setTextColor(textView.context.getColor(android.R.color.black))
            }
            1 -> {
                textView.setBackgroundResource(R.drawable.tag_button_include)
                textView.setTextColor(textView.context.getColor(android.R.color.black))
            }
            2 -> {
                textView.setBackgroundResource(R.drawable.tag_button_exclude)
                textView.setTextColor(textView.context.getColor(android.R.color.black))
            }
        }
    }

    override fun getItemCount(): Int = tagList.size

    fun getIncludedTags(): List<String> {
        return tagList.filter { it.state == 1 }.map { it.tag.value }
    }

    fun getExcludedTags(): List<String> {
        return tagList.filter { it.state == 2 }.map { it.tag.value }
    }
}
