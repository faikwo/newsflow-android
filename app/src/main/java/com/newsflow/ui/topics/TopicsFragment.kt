package com.newsflow.ui.topics

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.newsflow.api.ApiRepository
import com.newsflow.api.ApiResult
import com.newsflow.data.Topic
import com.newsflow.databinding.FragmentTopicsBinding
import kotlinx.coroutines.launch

class TopicsFragment : Fragment() {

    private var _binding: FragmentTopicsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TopicsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTopicsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TopicsAdapter { topic, subscribe ->
            toggleSubscription(topic, subscribe)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        loadTopics()
    }

    private fun loadTopics() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val result = ApiRepository.getAllTopics()) {
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitGrouped(result.data.grouped)
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, "Failed to load topics: ${result.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun toggleSubscription(topic: Topic, subscribe: Boolean) {
        lifecycleScope.launch {
            val result = if (subscribe) ApiRepository.subscribe(topic.id) else ApiRepository.unsubscribe(topic.id)
            when (result) {
                is ApiResult.Success -> {
                    val msg = if (subscribe) "Subscribed to ${topic.name}" else "Unsubscribed from ${topic.name}"
                    Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                }
                is ApiResult.Error -> {
                    Snackbar.make(binding.root, "Failed: ${result.message}", Snackbar.LENGTH_SHORT).show()
                    adapter.revertToggle(topic.id)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
