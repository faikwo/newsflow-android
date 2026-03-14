package com.newsflow.ui.stats

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.newsflow.api.ApiRepository
import com.newsflow.api.ApiResult
import com.newsflow.databinding.FragmentStatsBinding
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStats()
    }

    private fun loadStats() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            // Load stats and affinity in parallel
            val statsResult = ApiRepository.getStats()
            val affinityResult = ApiRepository.getAffinity()

            binding.progressBar.visibility = View.GONE

            if (statsResult is ApiResult.Success) {
                val s = statsResult.data
                val interactions = s.interactions
                binding.tvReadCount.text = (interactions["read"] ?: 0).toString()
                binding.tvLikeCount.text = (interactions["like"] ?: 0).toString()
                binding.tvDislikeCount.text = (interactions["dislike"] ?: 0).toString()
                binding.tvSavedCount.text = (interactions["save_later"] ?: 0).toString()
                binding.tvSubscribedCount.text = "${s.subscribedTopics} topics subscribed"

                if (s.topLikedTopics.isNotEmpty()) {
                    val maxCount = s.topLikedTopics.maxOf { it.count }.coerceAtLeast(1)
                    val sb = StringBuilder()
                    s.topLikedTopics.take(10).forEach { t ->
                        val barLen = (t.count * 20 / maxCount).coerceIn(1, 20)
                        val bar = "█".repeat(barLen)
                        sb.appendLine("${t.icon ?: "📰"} ${t.name.padEnd(18)} $bar ${t.count}")
                    }
                    binding.tvTopTopics.text = sb.toString().trimEnd()
                } else {
                    binding.tvTopTopics.text = "Like some articles to see your top topics."
                }
            }

            if (affinityResult is ApiResult.Success) {
                val affinities = affinityResult.data.affinities
                if (affinities.isNotEmpty()) {
                    binding.cardAffinity.visibility = View.VISIBLE
                    val maxScore = affinities.maxOf { it.score }.coerceAtLeast(0.001)
                    val sb = StringBuilder()
                    affinities.take(12).forEach { a ->
                        val barLen = ((a.score / maxScore) * 20).toInt().coerceIn(1, 20)
                        val bar = "▓".repeat(barLen)
                        val pct = (a.score * 100).toInt()
                        sb.appendLine("${a.topicIcon ?: "📰"} ${a.topicName.padEnd(18)} $bar $pct%")
                    }
                    binding.tvAffinity.text = sb.toString().trimEnd()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
