package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentPrivacyBinding
import one.mixin.android.databinding.ViewTitleBinding
import one.mixin.android.extension.navTo
import one.mixin.android.session.Session

@AndroidEntryPoint
class PrivacyFragment : BaseSettingFragment<FragmentPrivacyBinding>() {
    companion object {
        const val TAG = "PrivacyFragment"

        fun newInstance() = PrivacyFragment()
    }

    private val viewModel by viewModels<SettingViewModel>()

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPrivacyBinding.inflate(inflater, container, false).apply {
            _titleBinding = ViewTitleBinding.bind(titleView)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleBinding.leftIb.setOnClickListener {
            activity?.onBackPressed()
        }
        binding.apply {
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
