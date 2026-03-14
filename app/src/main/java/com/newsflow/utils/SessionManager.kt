package com.newsflow.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Simple SharedPreferences-backed session store.
 * Provides both synchronous (blocking) reads for use in OkHttp interceptors
 * and coroutine-friendly suspending writes.
 */
object SessionManager {

    private const val PREFS_NAME = "newsflow_session"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_TOKEN = "token"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_IS_ADMIN = "is_admin"

    private lateinit var prefs: SharedPreferences

    fun init(ctx: Context) {
        prefs = ctx.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(serverUrl: String, token: String, username: String, isAdmin: Boolean, email: String? = null) {
        prefs.edit {
            putString(KEY_SERVER_URL, serverUrl)
            putString(KEY_TOKEN, token)
            putString(KEY_USERNAME, username)
            putBoolean(KEY_IS_ADMIN, isAdmin)
            if (email != null) putString(KEY_EMAIL, email)
        }
    }

    fun saveEmail(email: String) {
        prefs.edit { putString(KEY_EMAIL, email) }
    }

    fun saveServerUrl(url: String) {
        prefs.edit { putString(KEY_SERVER_URL, url) }
    }

    fun clearSession() {
        prefs.edit {
            remove(KEY_TOKEN)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            remove(KEY_IS_ADMIN)
        }
    }

    fun getServerUrlBlocking(): String? = prefs.getString(KEY_SERVER_URL, null)
    fun getTokenBlocking(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUsernameBlocking(): String? = prefs.getString(KEY_USERNAME, null)
    fun getEmailBlocking(): String? = prefs.getString(KEY_EMAIL, null)
    fun getIsAdminBlocking(): Boolean = prefs.getBoolean(KEY_IS_ADMIN, false)

    fun isLoggedIn(): Boolean = !getTokenBlocking().isNullOrBlank()
    fun hasServerUrl(): Boolean = !getServerUrlBlocking().isNullOrBlank()
}
