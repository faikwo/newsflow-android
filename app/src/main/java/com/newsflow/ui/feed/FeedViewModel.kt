package com.newsflow.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.newsflow.api.ApiRepository
import com.newsflow.api.ApiResult
import com.newsflow.data.Article
import com.newsflow.database.AppDatabase
import com.newsflow.database.ArticleDao
import com.newsflow.database.toArticle
import com.newsflow.database.toEntity
import com.newsflow.widget.NewsFlowWidgetProvider
import kotlinx.coroutines.launch

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val articleDao: ArticleDao = AppDatabase.getDatabase(application).articleDao()

    private val _articles = MutableLiveData<List<Article>>()
    val articles: LiveData<List<Article>> = _articles

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _refreshResult = MutableLiveData<String?>()
    val refreshResult: LiveData<String?> = _refreshResult

    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore

    private var currentPage = 1
    private var currentTopicId: Int? = null
    private var currentSearch: String? = null
    private var currentFilter: String? = null
    private var allArticles = mutableListOf<Article>()
    private var isLoadingMore = false
    private var hasLoadedFromCache = false

    init {
        // Load cached articles first
        loadFromCache()
    }

    private fun loadFromCache() {
        viewModelScope.launch {
            try {
                // Get cached articles from Room
                val cachedArticles = articleDao.getRecentArticles(100).map { it.toArticle() }
                if (cachedArticles.isNotEmpty() && !hasLoadedFromCache) {
                    allArticles.clear()
                    allArticles.addAll(cachedArticles)
                    _articles.value = allArticles.toList()
                    hasLoadedFromCache = true
                }
            } catch (e: Exception) {
                // Silently fail if cache can't be loaded
            }
        }
    }

    fun loadFeed(
        topicId: Int? = null,
        search: String? = null,
        filter: String? = null,
        reset: Boolean = true
    ) {
        if (reset) {
            currentPage = 1
            currentTopicId = topicId
            currentSearch = search
            currentFilter = filter
            allArticles.clear()
            _hasMore.value = true
            
            // Try to load from cache first if not already loaded
            if (!hasLoadedFromCache) {
                loadFromCache()
            }
        }
        if (isLoadingMore && !reset) return
        isLoadingMore = true
        if (currentPage == 1) _isLoading.value = true

        viewModelScope.launch {
            when (val result = ApiRepository.getFeed(
                page = currentPage,
                perPage = 20,
                topicId = currentTopicId,
                search = currentSearch,
                filter = currentFilter
            )) {
                is ApiResult.Success -> {
                    val fetchedArticles = result.data.articles
                    
                    if (reset) {
                        // Clear and add fresh articles
                        allArticles.clear()
                        allArticles.addAll(fetchedArticles)
                        
                        // Save to cache (on a background thread)
                        saveArticlesToCache(fetchedArticles)
                    } else {
                        // Append for pagination
                        allArticles.addAll(fetchedArticles)
                        
                        // Save new articles to cache
                        saveArticlesToCache(fetchedArticles)
                    }
                    
                    _articles.value = allArticles.toList()
                    _hasMore.value = result.data.hasMore
                    currentPage++
                    _error.value = null
                }
                is ApiResult.Error -> {
                    // If we have cached articles, don't show error
                    if (allArticles.isEmpty()) {
                        _error.value = result.message
                    }
                }
            }
            _isLoading.value = false
            isLoadingMore = false
        }
    }

    private suspend fun saveArticlesToCache(articles: List<Article>) {
        try {
            val entities = articles.map { it.toEntity() }
            articleDao.insertArticles(entities)
            
            // Update the widget with new articles
            NewsFlowWidgetProvider.updateWidget(getApplication())
        } catch (e: Exception) {
            // Silently fail if cache can't be saved
        }
    }

    fun loadMore() {
        if (_hasMore.value == true && !isLoadingMore) {
            loadFeed(currentTopicId, currentSearch, currentFilter, reset = false)
        }
    }

    fun refreshFromServer(topicId: Int? = null) {
        _isRefreshing.value = true
        viewModelScope.launch {
            when (val r = ApiRepository.refresh(topicId)) {
                is ApiResult.Success -> {
                    _refreshResult.value = "Fetched ${r.data.articlesFetched} new articles"
                    // Reload feed after refresh
                    loadFeed(currentTopicId, currentSearch, currentFilter, reset = true)
                }
                is ApiResult.Error -> {
                    _refreshResult.value = "Refresh failed: ${r.message}"
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun interact(articleId: Int, action: String) {
        viewModelScope.launch {
            // Send interaction to server first (source of truth)
            ApiRepository.interact(articleId, action)

            if (action == "hide") {
                // Remove immediately with animation - server already knows
                val hiddenArticle = allArticles.find { it.id == articleId }
                allArticles.removeAll { it.id == articleId }
                _articles.value = allArticles.toList()

                // Update cache - remove hidden article
                hiddenArticle?.let {
                    try {
                        articleDao.deleteArticle(it.id)
                    } catch (e: Exception) {
                        // Silently fail
                    }
                }
            } else {
                // For other actions (like, dislike, save), just update the action state
                val updated = allArticles.map { article ->
                    if (article.id == articleId) {
                        val newAction = when (action) {
                            "remove", "unsave" -> null
                            else -> action
                        }
                        article.copy(userAction = newAction)
                    } else article
                }
                allArticles.clear()
                allArticles.addAll(updated)
                _articles.value = allArticles.toList()

                // Update the cached article as well
                val updatedEntity = allArticles.find { it.id == articleId }
                updatedEntity?.let {
                    try {
                        articleDao.insertArticle(it.toEntity())
                    } catch (e: Exception) {
                        // Silently fail
                    }
                }
            }
        }
    }

    fun clearRefreshResult() { _refreshResult.value = null }
    fun clearError() { _error.value = null }
}