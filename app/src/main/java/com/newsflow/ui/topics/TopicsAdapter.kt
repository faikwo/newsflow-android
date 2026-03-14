package com.newsflow.ui.topics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.newsflow.data.Topic
import com.newsflow.databinding.ItemTopicBinding
import com.newsflow.databinding.ItemTopicHeaderBinding

internal sealed class TopicListItem {
    data class Header(val category: String) : TopicListItem()
    data class TopicItem(val topic: Topic, var subscribed: Boolean) : TopicListItem()
}

class TopicsAdapter(
    private val onToggle: (Topic, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<TopicListItem>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TOPIC = 1
    }

    fun submitGrouped(grouped: Map<String, List<Topic>>) {
        items.clear()
        grouped.entries.sortedBy { it.key }.forEach { (category, topics) ->
            items.add(TopicListItem.Header(category))
            topics.sortedBy { it.name }.forEach { topic ->
                items.add(TopicListItem.TopicItem(topic, topic.subscribed == 1))
            }
        }
        notifyDataSetChanged()
    }

    fun revertToggle(topicId: Int) {
        val idx = items.indexOfFirst { it is TopicListItem.TopicItem && it.topic.id == topicId }
        if (idx >= 0) {
            val item = items[idx] as TopicListItem.TopicItem
            item.subscribed = !item.subscribed
            notifyItemChanged(idx)
        }
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is TopicListItem.Header -> TYPE_HEADER
        is TopicListItem.TopicItem -> TYPE_TOPIC
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderVH(ItemTopicHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            TopicVH(ItemTopicBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TopicListItem.Header -> (holder as HeaderVH).bind(item.category)
            is TopicListItem.TopicItem -> (holder as TopicVH).bind(item, position)
        }
    }

    inner class HeaderVH(val binding: ItemTopicHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: String) { binding.tvCategory.text = category }
    }

    inner class TopicVH(val binding: ItemTopicBinding) : RecyclerView.ViewHolder(binding.root) {
        internal fun bind(item: TopicListItem.TopicItem, position: Int) {
            binding.tvIcon.text = item.topic.icon ?: "📰"
            binding.tvName.text = item.topic.name
            binding.tvArticleCount.text = "${item.topic.articleCount} articles"
            binding.switchSubscribe.setOnCheckedChangeListener(null)
            binding.switchSubscribe.isChecked = item.subscribed
            binding.switchSubscribe.setOnCheckedChangeListener { _, isChecked ->
                item.subscribed = isChecked
                onToggle(item.topic, isChecked)
            }
        }
    }
}
