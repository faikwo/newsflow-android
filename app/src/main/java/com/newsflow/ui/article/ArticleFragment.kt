package com.newsflow.ui.article

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.newsflow.R
import com.newsflow.api.ApiRepository
import com.newsflow.api.ApiResult
import com.newsflow.data.Article
import com.newsflow.databinding.FragmentArticleBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ArticleFragment : Fragment() {

    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!
    private lateinit var article: Article
    private var currentAction: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // API-level safe parcelable retrieval
        article = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable("article", Article::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable("article")!!
        }
        currentAction = article.userAction

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        bindArticle()
        setupActions()
    }

    private fun bindArticle() {
        val a = article
        binding.tvTitle.text = a.title
        binding.tvSource.text = a.source ?: ""
        binding.tvTopic.text = "${a.topicIcon ?: ""} ${a.topicName ?: ""}".trim()

        a.publishedAt?.let { dateStr ->
            try {
                val inFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outFmt = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
                binding.tvDate.text = inFmt.parse(dateStr.substringBefore("."))
                    ?.let { outFmt.format(it) } ?: ""
            } catch (e: Exception) {
                binding.tvDate.text = ""
            }
        }

        if (!a.imageUrl.isNullOrBlank()) {
            binding.ivHero.visibility = View.VISIBLE
            Glide.with(this).load(a.imageUrl).centerCrop().into(binding.ivHero)
        } else {
            binding.ivHero.visibility = View.GONE
        }

        val summary = a.aiSummary?.takeIf { it.isNotBlank() } ?: a.summary
        if (!summary.isNullOrBlank()) {
            binding.tvSummary.text = summary
            binding.tvSummary.visibility = View.VISIBLE
            binding.tvSummaryLabel.visibility = View.VISIBLE
            binding.tvSummaryLabel.text = if (a.aiSummary?.isNotBlank() == true) "✨ AI Summary" else "Summary"
        } else {
            binding.tvSummary.visibility = View.GONE
            binding.tvSummaryLabel.visibility = View.GONE
        }

        updateActionButtons()
    }

    private fun setupActions() {
        binding.btnOpenBrowser.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
            lifecycleScope.launch { ApiRepository.interact(article.id, "read") }
        }
        binding.btnShare.setOnClickListener { getShareLink() }
        binding.btnAiSummary.setOnClickListener { requestAiSummary() }
        binding.btnLike.setOnClickListener { toggleAction("like") }
        binding.btnDislike.setOnClickListener { toggleAction("dislike") }
        binding.btnSave.setOnClickListener { toggleAction("save_later") }
    }

    private fun toggleAction(action: String) {
        val req = if (currentAction == action) {
            if (action == "save_later") "unsave" else "remove"
        } else action

        lifecycleScope.launch {
            when (ApiRepository.interact(article.id, req)) {
                is ApiResult.Success -> {
                    currentAction = if (req == "remove" || req == "unsave") null else action
                    updateActionButtons()
                }
                is ApiResult.Error ->
                    Snackbar.make(binding.root, "Action failed", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateActionButtons() {
        val ctx = requireContext()
        fun tint(active: Boolean, color: Int) =
            ContextCompat.getColor(ctx, if (active) color else R.color.text_secondary)

        binding.btnLike.setColorFilter(tint(currentAction == "like", R.color.like_color))
        binding.btnDislike.setColorFilter(tint(currentAction == "dislike", R.color.dislike_color))
        binding.btnSave.setColorFilter(tint(currentAction == "save_later", R.color.save_color))
    }

    private fun requestAiSummary() {
        binding.btnAiSummary.isEnabled = false
        binding.aiSummaryProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = ApiRepository.summarize(article.id)) {
                is ApiResult.Success -> {
                    binding.tvSummary.text = r.data.summary
                    binding.tvSummary.visibility = View.VISIBLE
                    binding.tvSummaryLabel.text = "✨ AI Summary"
                    binding.tvSummaryLabel.visibility = View.VISIBLE
                }
                is ApiResult.Error ->
                    Snackbar.make(binding.root, "AI summary failed: ${r.message}", Snackbar.LENGTH_LONG).show()
            }
            binding.aiSummaryProgress.visibility = View.GONE
            binding.btnAiSummary.isEnabled = true
        }
    }

    private fun getShareLink() {
        lifecycleScope.launch {
            when (val r = ApiRepository.getShareToken(article.id)) {
                is ApiResult.Success -> {
                    val clipboard = requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("NewsFlow Share Link", r.data.url))
                    Snackbar.make(binding.root, "Share link copied!", Snackbar.LENGTH_SHORT).show()
                }
                is ApiResult.Error -> {
                    // Fallback to plain share intent
                    startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${article.title}\n\n${article.url}")
                        }, "Share article"
                    ))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
