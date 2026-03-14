package com.newsflow

import android.app.Application
import com.newsflow.database.AppDatabase
import com.newsflow.utils.SessionManager
import com.newsflow.utils.SettingsManager

class NewsFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        SettingsManager.init(this)
        // Initialize database early for widget access
        AppDatabase.getDatabase(this)
    }
}