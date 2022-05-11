package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentPrivacyBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.supportsOreo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class PrivacyFragment : BaseFragment(R.layout.fragment_privacy) {
    companion object {
        const val TAG = "PrivacyFragment"

        fun newInstance() = PrivacyFragment()
    }

    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentPrivacyBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressed()
            }
            viewModel.countBlockingUsers().observe(
                viewLifecycleOwner,
                {
                    it?.let { users ->
                        blockingTv.text = "${users.size}"
                    }
                }
            )

            blockedRl.setOnClickListener {
                navTo(SettingBlockedFragment.newInstance(), SettingBlockedFragment.TAG)
            }
            phoneNumberRl.setOnClickListener {
                navTo(PhoneNumberSettingFragment.newInstance(), PhoneNumberSettingFragment.TAG)
            }
            conversationRl.setOnClickListener {
                navTo(SettingConversationFragment.newInstance(), SettingConversationFragment.TAG)
            }

            contactRl.setOnClickListener {
                navTo(MobileContactFragment.newInstance(), MobileContactFragment.TAG)
            }

            supportsOreo(
                {
                    incognito.isVisible = true
                    incognitoFollower.isVisible = true
                    incognito.setContent(R.string.Incognito_Keyboard)
                    incognito.isChecked =
                        defaultSharedPreferences.getBoolean(Constants.Account.PREF_INCOGNITO_KEYBOARD, false)
                    incognito.setOnCheckedChangeListener { _, isChecked ->
                        defaultSharedPreferences.putBoolean(Constants.Account.PREF_INCOGNITO_KEYBOARD, isChecked)
                    }
                },
                {
                    incognito.isVisible = false
                    incognitoFollower.isVisible = false
                }
            )

            setLockDesc()
            lockRl.setOnClickListener {
                navTo(AppAuthSettingFragment.newInstance(), AppAuthSettingFragment.TAG)
            }
        }
    }

    fun setLockDesc() {
        binding.lockDescTv.text = getString(
            when (defaultSharedPreferences.getInt(Constants.Account.PREF_APP_AUTH, -1)) {
                0 -> R.string.Enable_immediately
                1 -> R.string.Enable_after_1_minute
                2 -> R.string.Enable_after_30_minutes
                else -> R.string.Disabled
            }
        )
    }
}
