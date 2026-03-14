package com.newsflow.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.newsflow.MainActivity
import com.newsflow.R
import com.newsflow.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AppWidgetProvider for NewsFlow home screen widget.
 * Shows the 3 latest articles with refresh functionality.
 */
class NewsFlowWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.newsflow.widget.REFRESH"
        const val ACTION_OPEN_ARTICLE = "com.newsflow.widget.OPEN_ARTICLE"
        const val EXTRA_ARTICLE_ID = "article_id"

        fun updateWidget(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, NewsFlowWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_newsflow)

            // Set up refresh button click
            val refreshIntent = Intent(context, NewsFlowWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

            // Load articles from database
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val articleDao = AppDatabase.getDatabase(context).articleDao()
                    val articles = articleDao.getRecentArticles(3)

                    withContext(Dispatchers.Main) {
                        if (articles.isEmpty()) {
                            views.setViewVisibility(R.id.tv_widget_empty, android.view.View.VISIBLE)
                            // Hide article views
                            views.setViewVisibility(R.id.tv_article_1, android.view.View.GONE)
                            views.setViewVisibility(R.id.tv_article_2, android.view.View.GONE)
                            views.setViewVisibility(R.id.tv_article_3, android.view.View.GONE)
                        } else {
                            views.setViewVisibility(R.id.tv_widget_empty, android.view.View.GONE)
                            views.setViewVisibility(R.id.tv_article_1, android.view.View.VISIBLE)
                            views.setViewVisibility(R.id.tv_article_2, android.view.View.VISIBLE)
                            views.setViewVisibility(R.id.tv_article_3, android.view.View.VISIBLE)

                            // Set article texts and click handlers
                            articles.getOrNull(0)?.let { article ->
                                views.setTextViewText(R.id.tv_article_1, article.title)
                                val intent1 = Intent(context, MainActivity::class.java).apply {
                                    action = ACTION_OPEN_ARTICLE
                                    putExtra(EXTRA_ARTICLE_ID, article.id)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                val pendingIntent1 = PendingIntent.getActivity(
                                    context, article.id, intent1,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                views.setOnClickPendingIntent(R.id.tv_article_1, pendingIntent1)
                            } ?: views.setViewVisibility(R.id.tv_article_1, android.view.View.GONE)

                            articles.getOrNull(1)?.let { article ->
                                views.setTextViewText(R.id.tv_article_2, article.title)
                                val intent2 = Intent(context, MainActivity::class.java).apply {
                                    action = ACTION_OPEN_ARTICLE
                                    putExtra(EXTRA_ARTICLE_ID, article.id)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                val pendingIntent2 = PendingIntent.getActivity(
                                    context, article.id, intent2,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                views.setOnClickPendingIntent(R.id.tv_article_2, pendingIntent2)
                            } ?: views.setViewVisibility(R.id.tv_article_2, android.view.View.GONE)

                            articles.getOrNull(2)?.let { article ->
                                views.setTextViewText(R.id.tv_article_3, article.title)
                                val intent3 = Intent(context, MainActivity::class.java).apply {
                                    action = ACTION_OPEN_ARTICLE
                                    putExtra(EXTRA_ARTICLE_ID, article.id)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                val pendingIntent3 = PendingIntent.getActivity(
                                    context, article.id, intent3,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                views.setOnClickPendingIntent(R.id.tv_article_3, pendingIntent3)
                            } ?: views.setViewVisibility(R.id.tv_article_3, android.view.View.GONE)
                        }

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    // Show empty state on error
                    withContext(Dispatchers.Main) {
                        views.setViewVisibility(R.id.tv_widget_empty, android.view.View.VISIBLE)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_REFRESH_WIDGET -> {
                // Trigger widget update
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, NewsFlowWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // First widget added
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Last widget removed
    }
}