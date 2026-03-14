package com.newsflow.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.newsflow.R
import com.newsflow.api.ApiRepository
import com.newsflow.api.ApiResult
import com.newsflow.databinding.FragmentFeedBinding
import com.newsflow.utils.SettingsManager
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: ArticleAdapter
    private var selectedTopicId: Int? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupRecyclerView()
        setupSwipeRefresh()
        setupSearch()
        setupTopicChips()
        setupFab()
        setupEmptyState()
        observeViewModel()
        loadTopicChips()
        
        // Load cached articles first, then fetch from server
        viewModel.loadFeed(reset = true)
    }

    private fun setupAdapter() {
        adapter = ArticleAdapter(
            onArticleClick = { article ->
                findNavController().navigate(
                    R.id.action_feed_to_article, bundleOf("article" to article)
                )
            },
            onLike    = { a -> viewModel.interact(a.id, if (a.userAction == "like") "remove" else "like") },
            onDislike = { a -> viewModel.interact(a.id, if (a.userAction == "dislike") "remove" else "dislike") },
            onHide    = { a -> viewModel.interact(a.id, "hide") },
            onSave    = { a -> viewModel.interact(a.id, if (a.userAction == "save_later") "unsave" else "save_later") },
            onImagePreload = { imageUrl -> preloadImage(imageUrl) }
        )
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        // Infinite scroll
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as LinearLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                val total = adapter.itemCount

                // Load more when approaching end
                if (dy > 0 && lastVisible >= total - 5) {
                    viewModel.loadMore()
                }

                // Smart image preloading - preload images 5-10 items ahead
                if (SettingsManager.getImagePreloadEnabled() && dy > 0) {
                    preloadImagesAhead(lastVisible, total)
                }
            }
        })

        // Setup swipe gestures
        setupSwipeGestures()
    }

    private fun setupSwipeGestures() {
        val swipeCallback = SwipeCallback(
            adapter = adapter,
            onLike = { position ->
                val article = adapter.currentList.getOrNull(position) ?: return@SwipeCallback
                viewModel.interact(article.id, if (article.userAction == "like") "remove" else "like")
            },
            onSave = { position ->
                val article = adapter.currentList.getOrNull(position) ?: return@SwipeCallback
                viewModel.interact(article.id, if (article.userAction == "save_later") "unsave" else "save_later")
            },
            onHide = { position ->
                val article = adapter.currentList.getOrNull(position) ?: return@SwipeCallback
                viewModel.interact(article.id, "hide")
            }
        )
        
        itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper?.attachToRecyclerView(binding.recyclerView)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener { 
            viewModel.refreshFromServer(selectedTopicId) 
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.loadFeed(topicId = selectedTopicId, search = query?.takeIf { it.isNotBlank() }, reset = true)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) viewModel.loadFeed(topicId = selectedTopicId, reset = true)
                return false
            }
        })
    }

    private fun setupTopicChips() {
        binding.chipAll.setOnCheckedChangeListener { _, checked ->
            if (checked) { 
                selectedTopicId = null
                viewModel.loadFeed(reset = true) 
            }
        }
    }

    private fun setupFab() {
        // Show/hide FAB based on user preference (default is OFF)
        val showFab = SettingsManager.getShowFabRefresh()
        binding.fabRefresh.visibility = if (showFab) View.VISIBLE else View.GONE
        
        binding.fabRefresh.setOnClickListener { 
            viewModel.refreshFromServer(selectedTopicId) 
        }
    }

    private fun setupEmptyState() {
        binding.emptyState.btnRefreshNow.setOnClickListener {
            viewModel.refreshFromServer(selectedTopicId)
        }
    }

    private fun preloadImagesAhead(currentPosition: Int, totalItems: Int) {
        val preloadRange = 5..10
        for (offset in preloadRange) {
            val targetPosition = currentPosition + offset
            if (targetPosition >= totalItems) break
            
            val article = adapter.currentList.getOrNull(targetPosition) ?: continue
            article.imageUrl?.let { url ->
                preloadImage(url)
            }
        }
    }

    private fun preloadImage(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .preload()
    }

    private fun loadTopicChips() {
        lifecycleScope.launch {
            when (val r = ApiRepository.getSubscribedTopics()) {
                is ApiResult.Success -> {
                    val topics = r.data.topics
                    // Remove all chips except "All"
                    while (binding.chipGroupTopics.childCount > 1) {
                        binding.chipGroupTopics.removeViewAt(1)
                    }
                    topics.forEach { topic ->
                        val chip = Chip(requireContext()).apply {
                            text = "${topic.icon ?: ""} ${topic.name}".trim()
                            isCheckable = true
                            setOnCheckedChangeListener { _, checked ->
                                if (checked) {
                                    selectedTopicId = topic.id
                                    viewModel.loadFeed(topicId = topic.id, reset = true)
                                }
                            }
                        }
                        binding.chipGroupTopics.addView(chip)
                    }
                }
                else -> {}
            }
        }
    }

    private fun observeViewModel() {
        viewModel.articles.observe(viewLifecycleOwner) { articles ->
            adapter.submitList(articles)
            updateEmptyState(articles.isEmpty() && viewModel.isLoading.value == false)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            // Only show empty state when not loading and articles are empty
            val articlesEmpty = viewModel.articles.value?.isEmpty() ?: true
            updateEmptyState(articlesEmpty && !loading)
        }
        viewModel.isRefreshing.observe(viewLifecycleOwner) { refreshing ->
            binding.swipeRefresh.isRefreshing = refreshing
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { 
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError() 
            }
        }
        viewModel.refreshResult.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearRefreshResult()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyState.root.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.tvEmpty.visibility = View.GONE
        } else {
            binding.emptyState.root.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}