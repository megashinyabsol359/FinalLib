package com.example.finallib.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

    class TagFilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val btnTag: Button = itemView as Button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagFilterViewHolder {
        val button = Button(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(12, 12, 12, 12)
            textSize = 13f
            setTextColor(parent.context.getColor(android.R.color.black))
            background = parent.context.getDrawable(R.drawable.tag_button_background)
        }
        return TagFilterViewHolder(button)
    }

    override fun onBindViewHolder(holder: TagFilterViewHolder, position: Int) {
        val tagItem = tagList[position]
        holder.btnTag.text = tagItem.tag.label
        updateButtonState(holder.btnTag, tagItem)

        holder.btnTag.setOnClickListener {
            tagItem.state = (tagItem.state + 1) % 3
            updateButtonState(holder.btnTag, tagItem)
            onTagFilterChanged()
        }
    }

    private fun updateButtonState(button: Button, tagItem: TagFilterItem) {
        when (tagItem.state) {
            0 -> {
                // Normal state
                button.isSelected = false
                button.tag = 0
                button.setBackgroundResource(R.drawable.tag_button_normal)
                button.setTextColor(button.context.getColor(android.R.color.black))
            }
            1 -> {
                // Include state (Green)
                button.isSelected = true
                button.tag = 1
                button.setBackgroundResource(R.drawable.tag_button_include)
                button.setTextColor(button.context.getColor(android.R.color.black))
            }
            2 -> {
                // Exclude state (Red)
                button.isSelected = false
                button.tag = 2
                button.setBackgroundResource(R.drawable.tag_button_exclude)
                button.setTextColor(button.context.getColor(android.R.color.black))
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
