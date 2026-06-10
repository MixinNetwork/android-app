package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSecurityBinding
import one.mixin.android.extension.navTo
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.tip.TipActivity
import one.mixin.android.ui.tip.TipType
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class SecurityFragment : BaseFragment(R.layout.fragment_security) {
    companion object {
        const val TAG = "SecurityFragment"

        fun newInstance() = SecurityFragment()
    }

    private val binding by viewBinding(FragmentSecurityBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            pinRl.setOnClickListener {
                if (Session.getAccount()?.hasPin == true) {
                    navTo(PinSettingFragment.newInstance(), PinSettingFragment.TAG)
                } else {
                    TipActivity.show(requireActivity(), TipType.Create)
                }
            }

            authRl.setOnClickListener {
                navTo(AuthenticationsFragment.newInstance(), AuthenticationsFragment.TAG)
            }

            recoveryRl.setOnClickListener {
                navTo(RecoveryFragment.newInstance(), RecoveryFragment.TAG)
            }

            logs.setOnClickListener {
                navTo(PinLogsFragment.newInstance(), PinLogsFragment.TAG)
            }
        }
    }
}
