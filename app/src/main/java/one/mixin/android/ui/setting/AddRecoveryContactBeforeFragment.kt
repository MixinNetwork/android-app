package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.setting.ui.page.AddRecoveryContactBeforePage
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class AddRecoveryContactBeforeFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "AddRecoveryContactBeforeFragment"

        fun newInstance(
        ): AddRecoveryContactBeforeFragment =
            AddRecoveryContactBeforeFragment().apply {

            }
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.isVisible = false
        binding.compose.setContent {
            AddRecoveryContactBeforePage ({
                parentFragmentManager.apply {
                    findFragmentByTag(AddRecoveryContactFragment.TAG)?.let {
                        parentFragmentManager.beginTransaction().remove(it).commit()
                    }
                    findFragmentByTag(EmergencyContactFragment.TAG)?.let {
                        parentFragmentManager.beginTransaction().remove(it).commit()
                    }
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }, {
                navTo(VerifyFragment.newInstance(VerifyFragment.FROM_EMERGENCY), VerifyFragment.TAG)
            })
        }
    }
}