package com.newsflow.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.newsflow.MainActivity
import com.newsflow.api.ApiRepository
import com.newsflow.api.ApiResult
import com.newsflow.databinding.ActivityLoginBinding
import com.newsflow.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isRegister = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            when (val r = ApiRepository.signupEnabled()) {
                is ApiResult.Success -> if (!r.data.enabled) {
                    binding.btnToggleMode.visibility = View.GONE
                }
                else -> {}
            }
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (username.isBlank() || password.isBlank()) {
                showError("Please fill in all fields"); return@setOnClickListener
            }
            if (isRegister) {
                val email = binding.etEmail.text.toString().trim()
                if (email.isBlank()) { showError("Please enter your email"); return@setOnClickListener }
                doRegister(username, email, password)
            } else {
                doLogin(username, password)
            }
        }

        binding.btnToggleMode.setOnClickListener {
            isRegister = !isRegister
            updateMode()
        }

        binding.btnForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isBlank()) {
                if (!isRegister) { isRegister = true; updateMode() }
                showError("Enter your email then tap Forgot Password")
                return@setOnClickListener
            }
            doForgotPassword(email)
        }

        binding.btnChangeServer.setOnClickListener {
            startActivity(Intent(this, ServerSetupActivity::class.java))
            finish()
        }

        updateMode()
    }

    private fun updateMode() {
        binding.tvTitle.text = if (isRegister) "Create Account" else "Sign In"
        binding.tilEmail.visibility = if (isRegister) View.VISIBLE else View.GONE
        binding.btnLogin.text = if (isRegister) "Register" else "Sign In"
        binding.btnToggleMode.text = if (isRegister) "Already have an account? Sign in" else "No account? Register"
        binding.btnForgotPassword.visibility = if (isRegister) View.GONE else View.VISIBLE
        binding.tvError.visibility = View.GONE
    }

    private fun doLogin(username: String, password: String) {
        setLoading(true)
        lifecycleScope.launch {
            when (val result = ApiRepository.login(username, password)) {
                is ApiResult.Success -> {
                    SessionManager.saveSession(
                        SessionManager.getServerUrlBlocking()!!,
                        result.data.accessToken,
                        result.data.username,
                        result.data.isAdmin
                    )
                    goToMain()
                }
                is ApiResult.Error -> { setLoading(false); showError(result.message) }
            }
        }
    }

    private fun doRegister(username: String, email: String, password: String) {
        setLoading(true)
        lifecycleScope.launch {
            when (val result = ApiRepository.register(username, email, password)) {
                is ApiResult.Success -> {
                    SessionManager.saveSession(
                        SessionManager.getServerUrlBlocking()!!,
                        result.data.accessToken,
                        result.data.username,
                        result.data.isAdmin
                    )
                    goToMain()
                }
                is ApiResult.Error -> { setLoading(false); showError(result.message) }
            }
        }
    }

    private fun doForgotPassword(email: String) {
        setLoading(true)
        lifecycleScope.launch {
            ApiRepository.forgotPassword(email)
            setLoading(false)
            showError("If that email is registered, a reset link will be sent.")
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.etUsername.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
    }
}
