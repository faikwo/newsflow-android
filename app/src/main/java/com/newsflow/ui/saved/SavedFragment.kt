package com.newsflow.ui.saved

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.newsflow.R
import com.newsflow.api.ApiRepository
import com.newsflow.api.ApiResult
import com.newsflow.databinding.FragmentSavedBinding
import com.newsflow.ui.feed.ArticleAdapter
import kotlinx.coroutines.launch

class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ArticleAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ArticleAdapter(
            onArticleClick = { article ->
                findNavController().navigate(R.id.action_saved_to_article, bundleOf("article" to article))
            },
            onLike = { article ->
                interact(article.id, if (article.userAction == "like") "remove" else "like")
            },
            onDislike = { article ->
                interact(article.id, if (article.userAction == "dislike") "remove" else "dislike")
            },
            onHide = { article -> interact(article.id, "hide") },
            onSave = { article ->
                // Unsave from this list
                lifecycleScope.launch {
                    ApiRepository.interact(article.id, "unsave")
                    loadSaved()
                }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        loadSaved()
    }

    private fun loadSaved() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val result = ApiRepository.getSavedArticles()) {
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(result.data.articles)
                    binding.tvEmpty.visibility = if (result.data.articles.isEmpty()) View.VISIBLE else View.GONE
                    binding.tvCount.text = "${result.data.total} saved"
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun interact(articleId: Int, action: String) {
        lifecycleScope.launch { ApiRepository.interact(articleId, action) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
