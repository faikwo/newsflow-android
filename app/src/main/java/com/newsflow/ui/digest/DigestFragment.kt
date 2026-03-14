package com.newsflow.ui.digest

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.newsflow.api.ApiRepository
import com.newsflow.api.ApiResult
import com.newsflow.data.DigestScheduleUpdate
import com.newsflow.databinding.FragmentDigestBinding
import kotlinx.coroutines.launch

class DigestFragment : Fragment() {

    private var _binding: FragmentDigestBinding? = null
    private val binding get() = _binding!!
    private var currentHour = 7
    private var currentMinute = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDigestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        loadSchedule()
        binding.btnPickTime.setOnClickListener { showTimePicker() }
        binding.btnSave.setOnClickListener { saveSchedule() }
        binding.btnSendNow.setOnClickListener { sendNow() }
    }

    private fun loadSchedule() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            when (val r = ApiRepository.getDigestSchedule()) {
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val s = r.data
                    binding.switchEnabled.isChecked = s.enabled
                    currentHour = s.hour
                    currentMinute = s.minute
                    updateTimeDisplay()
                    binding.etEmail.setText(s.email ?: "")
                    binding.tvLastSent.text = if (s.lastSent != null) "Last sent: ${s.lastSent}" else ""
                    binding.tvNextSend.text = if (s.nextSend != null) "Next: ${s.nextSend}" else ""
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, "Failed to load: ${r.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showTimePicker() {
        MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Select digest send time")
            .build()
            .also { picker ->
                picker.addOnPositiveButtonClickListener {
                    currentHour = picker.hour
                    currentMinute = picker.minute
                    updateTimeDisplay()
                }
                picker.show(childFragmentManager, "time_picker")
            }
    }

    private fun updateTimeDisplay() {
        val amPm = if (currentHour < 12) "AM" else "PM"
        val h12 = when { currentHour == 0 -> 12; currentHour > 12 -> currentHour - 12; else -> currentHour }
        binding.tvTime.text = String.format("%d:%02d %s", h12, currentMinute, amPm)
    }

    private fun saveSchedule() {
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            val update = DigestScheduleUpdate(
                enabled = binding.switchEnabled.isChecked,
                hour = currentHour,
                minute = currentMinute,
                email = binding.etEmail.text.toString().trim().ifBlank { null }
            )
            when (val r = ApiRepository.updateDigestSchedule(update)) {
                is ApiResult.Success -> { Snackbar.make(binding.root, "Schedule saved ✓", Snackbar.LENGTH_SHORT).show(); loadSchedule() }
                is ApiResult.Error -> Snackbar.make(binding.root, "Failed: ${r.message}", Snackbar.LENGTH_LONG).show()
            }
            binding.btnSave.isEnabled = true
        }
    }

    private fun sendNow() {
        binding.btnSendNow.isEnabled = false
        lifecycleScope.launch {
            when (val r = ApiRepository.sendDigestNow()) {
                is ApiResult.Success -> Snackbar.make(binding.root, r.data.message ?: "Digest queued!", Snackbar.LENGTH_LONG).show()
                is ApiResult.Error -> Snackbar.make(binding.root, "Failed: ${r.message}", Snackbar.LENGTH_LONG).show()
            }
            binding.btnSendNow.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
