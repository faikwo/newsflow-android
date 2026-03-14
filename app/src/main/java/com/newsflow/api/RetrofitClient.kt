package com.newsflow.api

import com.newsflow.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String = ""

    fun getApi(baseUrl: String, @Suppress("UNUSED_PARAMETER") token: String? = null): NewsFlowApi {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (retrofit == null || normalizedUrl != currentBaseUrl) {
            currentBaseUrl = normalizedUrl
            retrofit = buildRetrofit(normalizedUrl)
        }
        return retrofit!!.create(NewsFlowApi::class.java)
    }

    fun reset() {
        retrofit = null
        currentBaseUrl = ""
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        // Dynamic auth interceptor — reads the current token on every request
        // so login / logout changes are picked up without rebuilding the client.
        val authInterceptor = Interceptor { chain ->
            val token = SessionManager.getTokenBlocking()
            val request = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
