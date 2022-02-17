package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSecurityBinding
import one.mixin.android.extension.navTo
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class SecurityFragment : BaseFragment(R.layout.fragment_security) {
    companion object {
        const val TAG = "SecurityFragment"

        fun newInstance() = SecurityFragment()
    }

    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentSecurityBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressed()
            }
            pinRl.setOnClickListener {
                if (Session.getAccount()?.hasPin == true) {
                    navTo(PinSettingFragment.newInstance(), PinSettingFragment.TAG)
                } else {
                    navTo(WalletPasswordFragment.newInstance(false), WalletPasswordFragment.TAG)
                }
            }

            authRl.setOnClickListener {
                navTo(AuthenticationsFragment.newInstance(), AuthenticationsFragment.TAG)
            }

            emergencyRl.setOnClickListener {
                navTo(EmergencyContactFragment.newInstance(), EmergencyContactFragment.TAG)
            }

            logs.setOnClickListener {
                navTo(PinLogsFragment.newInstance(), PinLogsFragment.TAG)
            }
        }
    }
}
