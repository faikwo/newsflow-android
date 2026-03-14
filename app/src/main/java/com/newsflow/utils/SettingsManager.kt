package com.newsflow.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * User preferences manager for app settings.
 * Separate from SessionManager which handles authentication state.
 */
object SettingsManager {
    
    private const val PREFS_NAME = "newsflow_settings"
    
    // Keys
    private const val KEY_SHOW_FAB_REFRESH = "show_fab_refresh"
    private const val KEY_IMAGE_PRELOAD_ENABLED = "image_preload_enabled"
    private const val KEY_CACHE_DAYS = "cache_days"
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // FAB Refresh setting - default is OFF (false) since pull-to-refresh exists
    fun setShowFabRefresh(show: Boolean) {
        prefs.edit { putBoolean(KEY_SHOW_FAB_REFRESH, show) }
    }
    
    fun getShowFabRefresh(): Boolean = prefs.getBoolean(KEY_SHOW_FAB_REFRESH, false)
    
    // Image preloading setting - default is ON (true)
    fun setImagePreloadEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_IMAGE_PRELOAD_ENABLED, enabled) }
    }
    
    fun getImagePreloadEnabled(): Boolean = prefs.getBoolean(KEY_IMAGE_PRELOAD_ENABLED, true)
    
    // Cache retention days - default is 7 days
    fun setCacheDays(days: Int) {
        prefs.edit { putInt(KEY_CACHE_DAYS, days.coerceIn(1, 30)) }
    }
    
    fun getCacheDays(): Int = prefs.getInt(KEY_CACHE_DAYS, 7)
    
    fun clearAll() {
        prefs.edit { clear() }
    }
}