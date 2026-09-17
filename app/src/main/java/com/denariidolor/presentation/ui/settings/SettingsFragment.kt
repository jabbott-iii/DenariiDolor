package com.denariidolor.presentation.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.denariidolor.R
import com.denariidolor.data.local.preferences.EncryptedPreferencesManager
import com.denariidolor.databinding.FragmentSettingsBinding
import com.denariidolor.presentation.ui.auth.LoginActivity
import com.denariidolor.presentation.ui.common.BaseFragment
import com.denariidolor.util.Constants
import com.denariidolor.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var encryptedPreferencesManager: EncryptedPreferencesManager

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        binding.tvSessionTimeout.text = getString(
            R.string.settings_session_timeout,
            Constants.SESSION_TIMEOUT_MILLIS / 60_000
        )
        binding.tvPinStatus.text = getString(
            if (encryptedPreferencesManager.getPin() == null) {
                R.string.settings_pin_not_configured
            } else {
                R.string.settings_pin_configured
            }
        )
        binding.btnSignOut.setOnClickListener { navigateToLogin(clearPin = false) }
        binding.btnResetPin.setOnClickListener { navigateToLogin(clearPin = true) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun navigateToLogin(clearPin: Boolean) {
        if (clearPin) {
            encryptedPreferencesManager.clearPin()
        }
        sessionManager.invalidate()
        val activity = requireActivity()
        activity.startActivity(
            Intent(activity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        activity.finish()
    }
}
