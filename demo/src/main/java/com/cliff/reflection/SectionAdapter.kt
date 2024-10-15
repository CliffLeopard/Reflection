package com.cliff.reflection

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cliff.reflection.databinding.SectionItemBinding

class SectionAdapter(private val context: Context) :
    ListAdapter<Section, SectionAdapter.SectionViewHolder>(sectionDiff) {
    class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = SectionItemBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.section_item, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.binding.apply {
            val section = getItem(position)
            this.title.text = section.title
            this.title.setOnClickListener {
                section.action(it)
            }
        }
    }
}

val sectionDiff: DiffUtil.ItemCallback<Section> = object : DiffUtil.ItemCallback<Section>() {
    override fun areItemsTheSame(oldItem: Section, newItem: Section) =
        oldItem.title == newItem.title

    override fun areContentsTheSame(oldItem: Section, newItem: Section) =
        oldItem.title == newItem.title
}