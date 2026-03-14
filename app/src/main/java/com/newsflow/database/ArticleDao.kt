package com.newsflow.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    
    @Query("SELECT * FROM articles ORDER BY published_at DESC")
    fun getAllArticles(): Flow<List<ArticleEntity>>
    
    @Query("SELECT * FROM articles WHERE id = :articleId")
    suspend fun getArticleById(articleId: Int): ArticleEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)
    
    @Query("DELETE FROM articles WHERE id = :articleId")
    suspend fun deleteArticle(articleId: Int)
    
    @Query("DELETE FROM articles")
    suspend fun deleteAllArticles()
    
    @Query("DELETE FROM articles WHERE cached_at < :timestamp")
    suspend fun deleteOldArticles(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getArticleCount(): Int
    
    @Query("SELECT * FROM articles ORDER BY published_at DESC LIMIT :limit")
    suspend fun getRecentArticles(limit: Int): List<ArticleEntity>
    
    @Query("SELECT * FROM articles WHERE user_action = 'save' ORDER BY published_at DESC")
    fun getSavedArticles(): Flow<List<ArticleEntity>>
}