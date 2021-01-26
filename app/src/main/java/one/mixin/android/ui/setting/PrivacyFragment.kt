package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.Account.PREF_INCOGNITO_KEYBOARD
import one.mixin.android.R
import one.mixin.android.databinding.FragmentPrivacyBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.supportsOreo
import one.mixin.android.session.Session
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
            pinRl.setOnClickListener {
                if (Session.getAccount()?.hasPin == true) {
                    navTo(PinSettingFragment.newInstance(), PinSettingFragment.TAG)
                } else {
                    navTo(WalletPasswordFragment.newInstance(false), WalletPasswordFragment.TAG)
                }
            }
            blockedRl.setOnClickListener {
                navTo(SettingBlockedFragment.newInstance(), SettingBlockedFragment.TAG)
            }
            phoneNumberRl.setOnClickListener {
                navTo(PhoneNumberSettingFragment.newInstance(), PhoneNumberSettingFragment.TAG)
            }
            conversationRl.setOnClickListener {
                navTo(SettingConversationFragment.newInstance(), SettingConversationFragment.TAG)
            }
            authRl.setOnClickListener {
                navTo(AuthenticationsFragment.newInstance(), AuthenticationsFragment.TAG)
            }
            supportsOreo({
                incognito.isVisible = true
                incognitoFollower.isVisible = true
                incognito.setContent(R.string.setting_incognito)
                incognito.isChecked =
                    defaultSharedPreferences.getBoolean(PREF_INCOGNITO_KEYBOARD, false)
                incognito.setOnCheckedChangeListener { _, isChecked ->
                    defaultSharedPreferences.putBoolean(PREF_INCOGNITO_KEYBOARD, isChecked)
                }
            }, {
                incognito.isVisible = false
                incognitoFollower.isVisible = false
            })

            logsRl.setOnClickListener {
                navTo(PinLogsFragment.newInstance(), PinLogsFragment.TAG)
            }
            emergencyRl.setOnClickListener {
                navTo(EmergencyContactFragment.newInstance(), EmergencyContactFragment.TAG)
            }
            contactRl.setOnClickListener {
                navTo(MobileContactFragment.newInstance(), MobileContactFragment.TAG)
            }
        }
    }
}
