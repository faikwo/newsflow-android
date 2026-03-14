package com.newsflow.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.newsflow.R
import com.newsflow.data.Article
import com.newsflow.databinding.ItemArticleBinding
import java.text.SimpleDateFormat
import java.util.*

class ArticleAdapter(
    private val onArticleClick: (Article) -> Unit,
    private val onLike: (Article) -> Unit,
    private val onDislike: (Article) -> Unit,
    private val onHide: (Article) -> Unit,
    private val onSave: (Article) -> Unit,
    private val onImagePreload: ((String) -> Unit)? = null
) : ListAdapter<Article, ArticleAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Article>() {
            override fun areItemsTheSame(a: Article, b: Article) = a.id == b.id
            override fun areContentsTheSame(a: Article, b: Article) = a == b
        }
    }

    inner class ViewHolder(val binding: ItemArticleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = getItem(position)
        val b = holder.binding
        val ctx = b.root.context

        b.tvTitle.text = article.title
        b.tvSource.text = article.source ?: ""
        b.tvTopic.text = "${article.topicIcon ?: ""} ${article.topicName ?: ""}".trim()

        val summary = article.aiSummary?.takeIf { it.isNotBlank() } ?: article.summary
        if (!summary.isNullOrBlank()) {
            b.tvSummary.text = summary
            b.tvSummary.visibility = View.VISIBLE
        } else {
            b.tvSummary.visibility = View.GONE
        }

        // Published date
        article.publishedAt?.let { dateStr ->
            try {
                val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val date = inFmt.parse(dateStr.substringBefore("."))
                b.tvDate.text = date?.let { outFmt.format(it) } ?: ""
                b.tvDate.visibility = View.VISIBLE
            } catch (e: Exception) {
                b.tvDate.visibility = View.GONE
            }
        } ?: run { b.tvDate.visibility = View.GONE }

        // Thumbnail with Glide
        if (!article.imageUrl.isNullOrBlank()) {
            b.ivThumbnail.visibility = View.VISIBLE
            Glide.with(ctx)
                .load(article.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_article_placeholder)
                .error(R.drawable.ic_article_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(b.ivThumbnail)
                
            // Trigger image preload for items coming into view
            onImagePreload?.invoke(article.imageUrl)
        } else {
            b.ivThumbnail.visibility = View.GONE
            Glide.with(ctx).clear(b.ivThumbnail)
        }

        // Action button states
        val liked = article.userAction == "like"
        val disliked = article.userAction == "dislike"
        val hidden = article.userAction == "hide"
        val saved = article.userAction == "save_later"

        fun tint(active: Boolean, activeColor: Int, inactiveColor: Int = R.color.text_secondary) =
            ContextCompat.getColor(ctx, if (active) activeColor else inactiveColor)

        b.btnLike.setColorFilter(tint(liked, R.color.like_color))
        b.btnDislike.setColorFilter(tint(disliked, R.color.dislike_color))
        b.btnHide.setColorFilter(tint(hidden, R.color.hide_color))
        b.btnSave.setColorFilter(tint(saved, R.color.save_color))

        b.root.setOnClickListener { onArticleClick(article) }
        b.btnLike.setOnClickListener { onLike(article) }
        b.btnDislike.setOnClickListener { onDislike(article) }
        b.btnHide.setOnClickListener { onHide(article) }
        b.btnSave.setOnClickListener { onSave(article) }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        // Clear Glide request to prevent image loading issues
        Glide.with(holder.binding.root.context).clear(holder.binding.ivThumbnail)
    }
}