package com.newsflow.api

import com.newsflow.data.*
import com.newsflow.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = -1) : ApiResult<Nothing>()
}

object ApiRepository {

    private fun api(): NewsFlowApi {
        val url = SessionManager.getServerUrlBlocking()
            ?: throw IllegalStateException("No server URL configured")
        return RetrofitClient.getApi(url)
    }

    private suspend fun <T> call(block: suspend NewsFlowApi.() -> Response<T>): ApiResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = api().block()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) ApiResult.Success(body)
                    else ApiResult.Error("Empty response", response.code())
                } else {
                    val errorMsg = response.errorBody()?.string()?.let { raw ->
                        Regex(""""detail"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.get(1) ?: raw
                    } ?: "HTTP ${response.code()}"
                    ApiResult.Error(errorMsg, response.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }

    // ── Health check (used before login, so URL may not be saved yet) ─────────
    suspend fun health(baseUrl: String): ApiResult<Map<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(baseUrl)
                val r = api.health()
                if (r.isSuccessful && r.body() != null) ApiResult.Success(r.body()!!)
                else ApiResult.Error("HTTP ${r.code()}", r.code())
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Connection failed")
            }
        }

    // ── Auth ──────────────────────────────────────────────────────────────────
    suspend fun signupEnabled(): ApiResult<SignupEnabledResponse> = call { signupEnabled() }

    suspend fun login(username: String, password: String): ApiResult<TokenResponse> =
        withContext(Dispatchers.IO) {
            try {
                val r = api().login(username, password)
                if (r.isSuccessful && r.body() != null) ApiResult.Success(r.body()!!)
                else {
                    val msg = r.errorBody()?.string()?.let { raw ->
                        Regex(""""detail"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.get(1) ?: raw
                    } ?: "Invalid credentials"
                    ApiResult.Error(msg, r.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Login failed")
            }
        }

    suspend fun register(username: String, email: String, password: String): ApiResult<TokenResponse> =
        withContext(Dispatchers.IO) {
            try {
                val r = api().register(RegisterRequest(username, email, password))
                if (r.isSuccessful && r.body() != null) ApiResult.Success(r.body()!!)
                else {
                    val msg = r.errorBody()?.string()?.let { raw ->
                        Regex(""""detail"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.get(1) ?: raw
                    } ?: "Registration failed"
                    ApiResult.Error(msg, r.code())
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Registration failed")
            }
        }

    suspend fun getMe(): ApiResult<MeResponse> = call { me() }
    suspend fun deleteOwnAccount(): ApiResult<StatusResponse> = call { deleteOwnAccount() }
    suspend fun listUsers(): ApiResult<AdminUsersResponse> = call { listUsers() }
    suspend fun adminUpdateUser(id: Int, update: AdminUserUpdate): ApiResult<StatusResponse> =
        call { adminUpdateUser(id, update) }
    suspend fun adminDeleteUser(id: Int): ApiResult<StatusResponse> = call { adminDeleteUser(id) }
    suspend fun forgotPassword(email: String): ApiResult<StatusResponse> =
        call { forgotPassword(mapOf("email" to email)) }

    // ── Feed ──────────────────────────────────────────────────────────────────
    suspend fun getFeed(
        page: Int = 1,
        perPage: Int = 20,
        topicId: Int? = null,
        search: String? = null,
        filter: String? = null
    ): ApiResult<FeedResponse> = call { getFeed(page, perPage, topicId, search, filter) }

    suspend fun getSavedArticles(page: Int = 1): ApiResult<SavedArticlesResponse> =
        call { getSavedArticles(page) }

    suspend fun interact(articleId: Int, action: String): ApiResult<StatusResponse> =
        call { interact(articleId, InteractionRequest(action)) }

    suspend fun refresh(topicId: Int? = null): ApiResult<RefreshResponse> =
        call { refresh(topicId) }

    suspend fun summarize(articleId: Int): ApiResult<SummarizeResponse> =
        call { summarize(articleId) }

    suspend fun getShareToken(articleId: Int): ApiResult<ShareTokenResponse> =
        call { getShareToken(articleId) }

    suspend fun getAffinity(): ApiResult<AffinityResponse> = call { getAffinity() }

    // ── Topics ────────────────────────────────────────────────────────────────
    suspend fun getAllTopics(): ApiResult<TopicsResponse> = call { getAllTopics() }
    suspend fun getSubscribedTopics(): ApiResult<SubscribedTopicsResponse> =
        call { getSubscribedTopics() }
    suspend fun subscribe(topicId: Int): ApiResult<StatusResponse> = call { subscribe(topicId) }
    suspend fun unsubscribe(topicId: Int): ApiResult<StatusResponse> = call { unsubscribe(topicId) }

    // ── Settings ──────────────────────────────────────────────────────────────
    suspend fun getSettings(): ApiResult<UserSettings> = call { getSettings() }
    suspend fun updateSettings(update: UserSettingsUpdate): ApiResult<StatusResponse> =
        call { updateSettings(update) }
    suspend fun testOllama(): ApiResult<OllamaTestResult> = call { testOllama() }

    // ── Stats ─────────────────────────────────────────────────────────────────
    suspend fun getStats(): ApiResult<StatsResponse> = call { getStats() }

    // ── Digest ────────────────────────────────────────────────────────────────
    suspend fun getDigestSchedule(): ApiResult<DigestSchedule> = call { getDigestSchedule() }
    suspend fun updateDigestSchedule(update: DigestScheduleUpdate): ApiResult<StatusResponse> =
        call { updateDigestSchedule(update) }
    suspend fun sendDigestNow(): ApiResult<DigestSendResponse> = call { sendDigestNow() }
}
