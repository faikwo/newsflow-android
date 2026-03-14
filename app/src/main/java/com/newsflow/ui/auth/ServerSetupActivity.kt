package com.newsflow.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.newsflow.MainActivity
import com.newsflow.api.ApiRepository
import com.newsflow.api.ApiResult
import com.newsflow.databinding.ActivityServerSetupBinding
import com.newsflow.utils.SessionManager
import kotlinx.coroutines.launch

class ServerSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SessionManager.getServerUrlBlocking()?.let { binding.etServerUrl.setText(it) }

        binding.btnConnect.setOnClickListener {
            val raw = binding.etServerUrl.text.toString().trim()
            if (raw.isBlank()) {
                binding.tilServerUrl.error = "Enter your NewsFlow server URL"
                return@setOnClickListener
            }
            val url = if (raw.startsWith("http")) raw else "http://$raw"
            testConnection(url)
        }
    }

    private fun testConnection(url: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnConnect.isEnabled = false
        binding.tvStatus.visibility = View.GONE

        lifecycleScope.launch {
            when (val result = ApiRepository.health(url)) {
                is ApiResult.Success -> {
                    SessionManager.saveServerUrl(url)
                    if (SessionManager.isLoggedIn()) {
                        startActivity(Intent(this@ServerSetupActivity, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this@ServerSetupActivity, LoginActivity::class.java))
                    }
                    finish()
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnConnect.isEnabled = true
                    binding.tvStatus.visibility = View.VISIBLE
                    binding.tvStatus.text =
                        "Could not connect: ${result.message}\n\nCheck that your server is running and the URL is correct (e.g. http://192.168.1.100:3000)"
                }
            }
        }
    }
}
