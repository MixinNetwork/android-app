package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.setting.ui.page.RecoveryKitPage
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class RecoveryFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"

        fun newInstance(
        ): RecoveryFragment =
            RecoveryFragment().apply {

            }
    }

    private val mobileViewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentComposeBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.isVisible = false
        binding.compose.setContent {
            RecoveryKitPage({
                navTo(VerifyFragment.newInstance(VerifyFragment.FROM_PHONE), VerifyFragment.TAG)
            }, {
                navTo(MnemonicPhraseBackupFragment.newInstance(), MnemonicPhraseBackupFragment.TAG)
            }, {
                navTo(EmergencyContactFragment.newInstance(), EmergencyContactFragment.TAG)
            })
        }
    }
}