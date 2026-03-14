package com.newsflow.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.newsflow.data.Article

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey
    val id: Int,
    
    val title: String,
    val summary: String?,
    val url: String,
    
    @ColumnInfo(name = "ai_summary")
    val aiSummary: String?,
    
    @ColumnInfo(name = "image_url")
    val imageUrl: String?,
    
    @ColumnInfo(name = "published_at")
    val publishedAt: String?,
    
    val source: String?,
    
    @ColumnInfo(name = "topic_name")
    val topicName: String?,
    
    @ColumnInfo(name = "topic_icon")
    val topicIcon: String?,
    
    @ColumnInfo(name = "user_action")
    val userAction: String?,
    
    val score: Double?,
    
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)

fun ArticleEntity.toArticle(): Article = Article(
    id = id,
    title = title,
    summary = summary,
    url = url,
    aiSummary = aiSummary,
    imageUrl = imageUrl,
    publishedAt = publishedAt,
    source = source,
    topicName = topicName,
    topicIcon = topicIcon,
    userAction = userAction,
    score = score
)

fun Article.toEntity(): ArticleEntity = ArticleEntity(
    id = id,
    title = title,
    summary = summary,
    url = url,
    aiSummary = aiSummary,
    imageUrl = imageUrl,
    publishedAt = publishedAt,
    source = source,
    topicName = topicName,
    topicIcon = topicIcon,
    userAction = userAction,
    score = score
)