package com.newsflow.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.newsflow.BuildConfig
import com.newsflow.R
import com.newsflow.api.ApiRepository
import com.newsflow.api.ApiResult
import com.newsflow.data.AdminUserUpdate
import com.newsflow.data.UserSettingsUpdate
import com.newsflow.databinding.FragmentSettingsBinding
import com.newsflow.ui.auth.LoginActivity
import com.newsflow.ui.auth.ServerSetupActivity
import com.newsflow.utils.SessionManager
import com.newsflow.utils.SettingsManager
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var isAdmin = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isAdmin = SessionManager.getIsAdminBlocking()
        binding.tvUsername.text = "Signed in as: ${SessionManager.getUsernameBlocking()}"
        binding.tvServerUrl.text = "Server: ${SessionManager.getServerUrlBlocking()}"

        if (!isAdmin) binding.cardAdmin.visibility = View.GONE

        loadSettings()
        setupListeners()
        loadPreferences()
        setupAboutSection()
    }

    private fun loadSettings() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val result = ApiRepository.getSettings()) {
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val s = result.data
                    binding.etOllamaUrl.setText(s.ollamaUrl ?: "")
                    binding.etOllamaModel.setText(s.ollamaModel ?: "")
                    binding.etRefreshInterval.setText((s.refreshIntervalMinutes ?: 60).toString())
                    binding.etMaxArticles.setText((s.maxArticlesPerTopic ?: 20).toString())
                    binding.switchAutoSummarize.isChecked = s.autoSummarize ?: false
                    binding.etNewsApiKey.setText(s.newsApiKey ?: "")
                    if (isAdmin) binding.switchAllowSignups.isChecked = s.allowSignups ?: true
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    snack("Failed to load settings: ${result.message}")
                }
            }
        }
    }

    private fun loadPreferences() {
        // Load local preferences
        binding.switchShowFabRefresh.isChecked = SettingsManager.getShowFabRefresh()
        binding.switchImagePreload.isChecked = SettingsManager.getImagePreloadEnabled()
    }

    private fun setupListeners() {
        binding.btnDigest.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_digest)
        }
        binding.btnTestOllama.setOnClickListener { testOllama() }
        binding.btnSaveSettings.setOnClickListener { saveSettings() }

        binding.btnChangeServer.setOnClickListener {
            startActivity(Intent(requireContext(), ServerSetupActivity::class.java))
            requireActivity().finish()
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ -> logout() }
                .setNegativeButton("Cancel", null).show()
        }

        binding.btnDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("This permanently deletes your account and all data. This cannot be undone.")
                .setPositiveButton("Delete") { _, _ -> deleteAccount() }
                .setNegativeButton("Cancel", null).show()
        }

        // Preference toggles
        binding.switchShowFabRefresh.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setShowFabRefresh(checked)
        }

        binding.switchImagePreload.setOnCheckedChangeListener { _, checked ->
            SettingsManager.setImagePreloadEnabled(checked)
        }

        if (isAdmin) binding.btnManageUsers.setOnClickListener { showAdminUsersDialog() }
    }

    private fun setupAboutSection() {
        // Set version
        binding.tvVersion.text = "${getString(R.string.version)} ${BuildConfig.VERSION_NAME}"

        // License click
        binding.btnLicense.setOnClickListener {
            showLicenseDialog()
        }

        // Third-party libraries click
        binding.btnThirdParty.setOnClickListener {
            showThirdPartyLibrariesDialog()
        }

        // Source code click
        binding.btnSourceCode.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/faikwo/newsflow-android"))
            startActivity(intent)
        }
    }

    private fun showLicenseDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.license)
            .setMessage("""
                Apache License 2.0
                
                Licensed under the Apache License, Version 2.0 (the "License");
                you may not use this file except in compliance with the License.
                You may obtain a copy of the License at:
                
                http://www.apache.org/licenses/LICENSE-2.0
                
                Unless required by applicable law or agreed to in writing, software
                distributed under the License is distributed on an "AS IS" BASIS,
                WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                See the License for the specific language governing permissions and
                limitations under the License.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .setNeutralButton("View Online") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.apache.org/licenses/LICENSE-2.0")))
            }
            .show()
    }

    private fun showThirdPartyLibrariesDialog() {
        val libraries = arrayOf(
            "AndroidX - Apache 2.0",
            "Retrofit - Apache 2.0",
            "OkHttp - Apache 2.0",
            "Glide - Apache 2.0",
            "Kotlin Coroutines - Apache 2.0",
            "Room - Apache 2.0",
            "Material Components - Apache 2.0",
            "Navigation Component - Apache 2.0"
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.third_party_libraries)
            .setItems(libraries) { _, _ -> }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun testOllama() {
        binding.btnTestOllama.isEnabled = false
        binding.tvOllamaStatus.visibility = View.VISIBLE
        binding.tvOllamaStatus.text = "Testing…"
        lifecycleScope.launch {
            when (val r = ApiRepository.testOllama()) {
                is ApiResult.Success -> {
                    val models = r.data.models?.joinToString(", ") ?: "none"
                    binding.tvOllamaStatus.text = "✅ Connected! Models: $models"
                }
                is ApiResult.Error -> binding.tvOllamaStatus.text = "❌ Failed: ${r.message}"
            }
            binding.btnTestOllama.isEnabled = true
        }
    }

    private fun saveSettings() {
        val update = UserSettingsUpdate(
            ollamaUrl = binding.etOllamaUrl.text.toString().trim().ifBlank { null },
            ollamaModel = binding.etOllamaModel.text.toString().trim().ifBlank { null },
            refreshIntervalMinutes = binding.etRefreshInterval.text.toString().toIntOrNull(),
            maxArticlesPerTopic = binding.etMaxArticles.text.toString().toIntOrNull(),
            autoSummarize = binding.switchAutoSummarize.isChecked,
            newsApiKey = binding.etNewsApiKey.text.toString().trim().ifBlank { null },
            allowSignups = if (isAdmin) binding.switchAllowSignups.isChecked else null
        )
        binding.btnSaveSettings.isEnabled = false
        lifecycleScope.launch {
            when (val r = ApiRepository.updateSettings(update)) {
                is ApiResult.Success -> snack("Settings saved ✓")
                is ApiResult.Error -> snack("Failed: ${r.message}")
            }
            binding.btnSaveSettings.isEnabled = true
        }
    }

    private fun logout() {
        SessionManager.clearSession()
        SettingsManager.clearAll()
        navigateToLogin()
    }

    private fun deleteAccount() {
        lifecycleScope.launch {
            when (val r = ApiRepository.deleteOwnAccount()) {
                is ApiResult.Success -> { 
                    SessionManager.clearSession()
                    SettingsManager.clearAll()
                    navigateToLogin() 
                }
                is ApiResult.Error -> snack("Failed: ${r.message}")
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }

    private fun showAdminUsersDialog() {
        lifecycleScope.launch {
            when (val r = ApiRepository.listUsers()) {
                is ApiResult.Success -> {
                    val users = r.data.users
                    val names = users.map {
                        "${it.username} (${it.email})${if (it.isAdmin) " 👑" else ""}"
                    }.toTypedArray()
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Manage Users")
                        .setItems(names) { _, idx -> showUserActionsDialog(users[idx]) }
                        .setNegativeButton("Close", null).show()
                }
                is ApiResult.Error -> snack("Failed: ${r.message}")
            }
        }
    }

    private fun showUserActionsDialog(user: com.newsflow.data.AdminUser) {
        val actions = arrayOf(
            if (user.isAdmin) "Remove Admin" else "Make Admin",
            "Delete User"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(user.username)
            .setItems(actions) { _, idx ->
                when (idx) {
                    0 -> lifecycleScope.launch {
                        when (val r = ApiRepository.adminUpdateUser(
                            user.id, AdminUserUpdate(isAdmin = !user.isAdmin)
                        )) {
                            is ApiResult.Success -> snack("Updated ${user.username}")
                            is ApiResult.Error -> snack("Failed: ${r.message}")
                        }
                    }
                    1 -> MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete ${user.username}?")
                        .setMessage("This cannot be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            lifecycleScope.launch {
                                when (val r = ApiRepository.adminDeleteUser(user.id)) {
                                    is ApiResult.Success -> snack("User deleted")
                                    is ApiResult.Error -> snack("Failed: ${r.message}")
                                }
                            }
                        }
                        .setNegativeButton("Cancel", null).show()
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun snack(msg: String) = Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}