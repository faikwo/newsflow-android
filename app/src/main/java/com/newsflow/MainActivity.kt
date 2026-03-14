package com.newsflow

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.newsflow.database.AppDatabase
import com.newsflow.database.toArticle
import com.newsflow.databinding.ActivityMainBinding
import com.newsflow.ui.auth.LoginActivity
import com.newsflow.ui.auth.ServerSetupActivity
import com.newsflow.utils.SessionManager
import com.newsflow.widget.NewsFlowWidgetProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            !SessionManager.hasServerUrl() -> {
                startActivity(Intent(this, ServerSetupActivity::class.java))
                finish(); return
            }
            !SessionManager.isLoggedIn() -> {
                startActivity(Intent(this, LoginActivity::class.java))
                finish(); return
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        // Handle widget article open intent
        handleWidgetIntent(navController)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navHostFragment?.navController?.let { handleWidgetIntent(it) }
    }

    private fun handleWidgetIntent(navController: androidx.navigation.NavController) {
        if (intent?.action == NewsFlowWidgetProvider.ACTION_OPEN_ARTICLE) {
            val articleId = intent.getIntExtra(NewsFlowWidgetProvider.EXTRA_ARTICLE_ID, -1)
            if (articleId != -1) {
                // Fetch article from database and navigate to detail
                lifecycleScope.launch {
                    try {
                        val article = AppDatabase.getDatabase(this@MainActivity)
                            .articleDao()
                            .getArticleById(articleId)
                            ?.toArticle()
                        
                        article?.let {
                            // Clear the intent action to prevent re-navigation
                            intent.action = null
                            
                            // Navigate to article detail
                            navController.navigate(
                                R.id.action_feed_to_article,
                                bundleOf("article" to it)
                            )
                        }
                    } catch (e: Exception) {
                        // Article not found in cache, navigate to feed
                        navController.navigate(R.id.feedFragment)
                    }
                }
            }
        }
    }
}