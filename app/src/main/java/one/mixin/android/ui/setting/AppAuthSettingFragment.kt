package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAppAuthSettingBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.auth.showAppAuthPrompt
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class AppAuthSettingFragment : BaseFragment(R.layout.fragment_app_auth_setting) {
    companion object {
        const val TAG = "AppAuthSettingFragment"

        fun newInstance() = AppAuthSettingFragment()
    }

    private val binding by viewBinding(FragmentAppAuthSettingBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressed()
            }
            unlockSwitch.isClickable = false
            unlockRl.setOnClickListener {
                val isSupportWithErrorInfo = BiometricUtil.isSupportWithErrorInfo(requireContext(), BiometricManager.Authenticators.BIOMETRIC_WEAK)
                val isSupport = isSupportWithErrorInfo.first
                if (!isSupport) {
                    isSupportWithErrorInfo.second?.let {
                        binding.errorTv.text = it
                        binding.errorTv.isVisible = true
                    }
                    refresh(-1)
                } else {
                    binding.errorTv.isVisible = false
                }
                if (unlockSwitch.isChecked) {
                    refresh(-1)
                } else {
                    showAppAuthPrompt(
                        this@AppAuthSettingFragment.requireActivity(),
                        getString(R.string.fingerprint_confirm),
                        getString(R.string.Cancel),
                        authCallback
                    )
                }
            }
            autoGroup.setOnCheckedChangeListener { _, checkedId ->
                val index = getIndexById(checkedId)
                refresh(index)
            }
            val appAuth = defaultSharedPreferences.getInt(Constants.Account.PREF_APP_AUTH, -1)
            refresh(appAuth)
        }
    }

    private fun refresh(appAuth: Int) {
        defaultSharedPreferences.putInt(Constants.Account.PREF_APP_AUTH, appAuth)
        if (viewDestroyed()) return

        binding.apply {
            if (appAuth == -1) {
                unlockSwitch.isChecked = false
                autoTitle.isVisible = false
                autoGroup.isVisible = false
            } else {
                unlockSwitch.isChecked = true
                autoTitle.isVisible = true
                autoGroup.isVisible = true
                setCheckedByIndex(appAuth)
            }
        }

        val privacyFragment = parentFragmentManager.findFragmentByTag(PrivacyFragment.TAG)
        (privacyFragment as? PrivacyFragment)?.setLockDesc()
    }

    private fun getIndexById(id: Int): Int {
        return when (id) {
            R.id.auto_immediately -> 0
            R.id.auto_after_1_minute -> 1
            R.id.auto_after_30_minutes -> 2
            else -> -1
        }
    }

    private fun setCheckedByIndex(index: Int) {
        when (index) {
            0 -> binding.autoImmediately.isChecked = true
            1 -> binding.autoAfter1Minute.isChecked = true
            2 -> binding.autoAfter30Minutes.isChecked = true
        }
    }

    private val authCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode == BiometricPrompt.ERROR_CANCELED ||
                errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT
            ) {
                refresh(-1)
            }
        }

        override fun onAuthenticationFailed() {
            refresh(-1)
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            refresh(0)
        }
    }
}
