package com.newsflow.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val username: String,
    @SerializedName("is_admin") val isAdmin: Boolean
)

data class SignupEnabledResponse(val enabled: Boolean)

data class MeResponse(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("is_admin") val isAdmin: Boolean,
    @SerializedName("created_at") val createdAt: String?
)

// ── Articles ──────────────────────────────────────────────────────────────────

@Parcelize
data class Article(
    val id: Int,
    val title: String,
    val summary: String?,
    val url: String,
    @SerializedName("ai_summary") val aiSummary: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("published_at") val publishedAt: String?,
    val source: String?,
    @SerializedName("topic_name") val topicName: String?,
    @SerializedName("topic_icon") val topicIcon: String?,
    @SerializedName("user_action") val userAction: String?,
    val score: Double?
) : Parcelable

data class FeedResponse(
    val articles: List<Article>,
    val total: Int,
    val page: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("has_more") val hasMore: Boolean
)

data class SavedArticlesResponse(
    val articles: List<Article>,
    val total: Int
)

data class InteractionRequest(val action: String)

data class RefreshResponse(
    val status: String,
    @SerializedName("articles_fetched") val articlesFetched: Int
)

data class ShareTokenResponse(
    val token: String,
    val url: String
)

data class SummarizeResponse(val summary: String)

// ── Topics ────────────────────────────────────────────────────────────────────

data class Topic(
    val id: Int,
    val name: String,
    val icon: String?,
    val category: String?,
    val subscribed: Int,
    @SerializedName("article_count") val articleCount: Int
)

data class TopicsResponse(
    val topics: List<Topic>,
    val grouped: Map<String, List<Topic>>
)

data class SubscribedTopicsResponse(val topics: List<Topic>)

// ── Settings ──────────────────────────────────────────────────────────────────

data class UserSettings(
    @SerializedName("ollama_url") val ollamaUrl: String?,
    @SerializedName("ollama_model") val ollamaModel: String?,
    @SerializedName("refresh_interval_minutes") val refreshIntervalMinutes: Int?,
    @SerializedName("max_articles_per_topic") val maxArticlesPerTopic: Int?,
    @SerializedName("auto_summarize") val autoSummarize: Boolean?,
    @SerializedName("newsapi_key") val newsApiKey: String?,
    @SerializedName("allow_signups") val allowSignups: Boolean?
)

data class UserSettingsUpdate(
    @SerializedName("ollama_url") val ollamaUrl: String? = null,
    @SerializedName("ollama_model") val ollamaModel: String? = null,
    @SerializedName("refresh_interval_minutes") val refreshIntervalMinutes: Int? = null,
    @SerializedName("max_articles_per_topic") val maxArticlesPerTopic: Int? = null,
    @SerializedName("auto_summarize") val autoSummarize: Boolean? = null,
    @SerializedName("newsapi_key") val newsApiKey: String? = null,
    @SerializedName("allow_signups") val allowSignups: Boolean? = null
)

data class OllamaTestResult(
    val status: String,
    val models: List<String>?
)

// ── Digest ────────────────────────────────────────────────────────────────────

data class DigestSchedule(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val email: String?,
    @SerializedName("last_sent") val lastSent: String?,
    @SerializedName("next_send") val nextSend: String?
)

data class DigestScheduleUpdate(
    val enabled: Boolean? = null,
    val hour: Int? = null,
    val minute: Int? = null,
    val email: String? = null
)

data class DigestSendResponse(val status: String, val message: String?)

// ── Stats ─────────────────────────────────────────────────────────────────────

data class TopicLike(
    val name: String,
    val icon: String?,
    val count: Int
)

data class StatsResponse(
    val interactions: Map<String, Int>,
    @SerializedName("top_liked_topics") val topLikedTopics: List<TopicLike>,
    @SerializedName("subscribed_topics") val subscribedTopics: Int
)

data class AffinityItem(
    @SerializedName("topic_id") val topicId: Int,
    @SerializedName("topic_name") val topicName: String,
    @SerializedName("topic_icon") val topicIcon: String?,
    val score: Double
)

data class AffinityResponse(val affinities: List<AffinityItem>)

// ── Admin ─────────────────────────────────────────────────────────────────────

data class AdminUser(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName("is_admin") val isAdmin: Boolean,
    @SerializedName("created_at") val createdAt: String?
)

data class AdminUsersResponse(val users: List<AdminUser>)

data class AdminUserUpdate(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    @SerializedName("is_admin") val isAdmin: Boolean? = null
)

// ── Generic ───────────────────────────────────────────────────────────────────

data class StatusResponse(val status: String, val message: String? = null)
