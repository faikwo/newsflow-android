package com.newsflow.api

import com.newsflow.data.*
import retrofit2.Response
import retrofit2.http.*

interface NewsFlowApi {

    // ── Health ────────────────────────────────────────────────────────────────
    @GET("api/health")
    suspend fun health(): Response<Map<String, String>>

    // ── Auth ──────────────────────────────────────────────────────────────────
    @GET("api/auth/signup-enabled")
    suspend fun signupEnabled(): Response<SignupEnabledResponse>

    @FormUrlEncoded
    @POST("api/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<TokenResponse>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<TokenResponse>

    @GET("api/auth/me")
    suspend fun me(): Response<MeResponse>

    @DELETE("api/auth/me")
    suspend fun deleteOwnAccount(): Response<StatusResponse>

    @GET("api/auth/users")
    suspend fun listUsers(): Response<AdminUsersResponse>

    @PATCH("api/auth/users/{id}")
    suspend fun adminUpdateUser(
        @Path("id") id: Int,
        @Body update: AdminUserUpdate
    ): Response<StatusResponse>

    @DELETE("api/auth/users/{id}")
    suspend fun adminDeleteUser(@Path("id") id: Int): Response<StatusResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: Map<String, String>): Response<StatusResponse>

    // ── Articles ──────────────────────────────────────────────────────────────
    @GET("api/articles/")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("topic_id") topicId: Int? = null,
        @Query("search") search: String? = null,
        @Query("filter") filter: String? = null
    ): Response<FeedResponse>

    @GET("api/articles/saved")
    suspend fun getSavedArticles(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<SavedArticlesResponse>

    @POST("api/articles/{id}/interact")
    suspend fun interact(
        @Path("id") articleId: Int,
        @Body request: InteractionRequest
    ): Response<StatusResponse>

    @POST("api/articles/refresh")
    suspend fun refresh(@Query("topic_id") topicId: Int? = null): Response<RefreshResponse>

    @POST("api/articles/{id}/summarize")
    suspend fun summarize(@Path("id") articleId: Int): Response<SummarizeResponse>

    @GET("api/articles/{id}/share-token")
    suspend fun getShareToken(@Path("id") articleId: Int): Response<ShareTokenResponse>

    @GET("api/articles/affinity")
    suspend fun getAffinity(): Response<AffinityResponse>

    // ── Topics ────────────────────────────────────────────────────────────────
    @GET("api/topics/")
    suspend fun getAllTopics(): Response<TopicsResponse>

    @GET("api/topics/subscribed")
    suspend fun getSubscribedTopics(): Response<SubscribedTopicsResponse>

    @POST("api/topics/{id}/subscribe")
    suspend fun subscribe(@Path("id") topicId: Int): Response<StatusResponse>

    @DELETE("api/topics/{id}/subscribe")
    suspend fun unsubscribe(@Path("id") topicId: Int): Response<StatusResponse>

    // ── Settings ──────────────────────────────────────────────────────────────
    @GET("api/settings/")
    suspend fun getSettings(): Response<UserSettings>

    @PUT("api/settings/")
    suspend fun updateSettings(@Body settings: UserSettingsUpdate): Response<StatusResponse>

    @GET("api/settings/test-ollama")
    suspend fun testOllama(): Response<OllamaTestResult>

    // ── Stats ─────────────────────────────────────────────────────────────────
    @GET("api/preferences/stats")
    suspend fun getStats(): Response<StatsResponse>

    // ── Digest ────────────────────────────────────────────────────────────────
    @GET("api/digest/schedule")
    suspend fun getDigestSchedule(): Response<DigestSchedule>

    @PUT("api/digest/schedule")
    suspend fun updateDigestSchedule(@Body update: DigestScheduleUpdate): Response<StatusResponse>

    @POST("api/digest/send-now")
    suspend fun sendDigestNow(): Response<DigestSendResponse>
}
