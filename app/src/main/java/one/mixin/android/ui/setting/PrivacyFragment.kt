package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_privacy.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.util.Session

@AndroidEntryPoint
class PrivacyFragment : BaseViewModelFragment<SettingViewModel>() {
    companion object {
        const val TAG = "PrivacyFragment"

        fun newInstance() = PrivacyFragment()
    }

    override fun getModelClass() = SettingViewModel::class.java

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_privacy, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener {
            activity?.onBackPressed()
        }
        viewModel.countBlockingUsers().observe(
            viewLifecycleOwner,
            Observer {
                it?.let { users ->
                    blocking_tv.text = "${users.size}"
                }
            }
        )
        pin_rl.setOnClickListener {
            if (Session.getAccount()?.hasPin == true) {
                navTo(PinSettingFragment.newInstance(), PinSettingFragment.TAG)
            } else {
                navTo(WalletPasswordFragment.newInstance(false), WalletPasswordFragment.TAG)
            }
        }
        blocked_rl.setOnClickListener {
            navTo(SettingBlockedFragment.newInstance(), SettingBlockedFragment.TAG)
        }
        phone_number_rl.setOnClickListener {
            navTo(PhoneNumberSettingFragment.newInstance(), PhoneNumberSettingFragment.TAG)
        }
        conversation_rl.setOnClickListener {
            navTo(SettingConversationFragment.newInstance(), SettingConversationFragment.TAG)
        }
        auth_rl.setOnClickListener {
            navTo(AuthenticationsFragment.newInstance(), AuthenticationsFragment.TAG)
        }
        logs_rl.setOnClickListener {
            navTo(PinLogsFragment.newInstance(), PinLogsFragment.TAG)
        }
        emergency_rl.setOnClickListener {
            navTo(EmergencyContactFragment.newInstance(), EmergencyContactFragment.TAG)
        }
        contact_rl.setOnClickListener {
            navTo(MobileContactFragment.newInstance(), MobileContactFragment.TAG)
        }
    }
}
