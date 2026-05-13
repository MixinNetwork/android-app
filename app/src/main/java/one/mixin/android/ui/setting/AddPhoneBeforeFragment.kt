package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.navTo
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.setting.ui.page.AddPhoneBeforePage
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class AddPhoneBeforeFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "AddPhoneBeforeFragment"
        private const val ARGS_SOURCE = "args_source"

        fun newInstance(
            source: String = AnalyticsTracker.AddPhoneSource.SETTINGS,
        ): AddPhoneBeforeFragment =
            AddPhoneBeforeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGS_SOURCE, source)
                }
            }
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.isVisible = false
        val source = requireArguments().getString(ARGS_SOURCE) ?: AnalyticsTracker.AddPhoneSource.SETTINGS
        AnalyticsTracker.trackAddPhoneStart(source)
        binding.compose.setContent {
            AddPhoneBeforePage(
                Session.hasPhone(),
                {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                },
                {
                    navTo(VerifyFragment.newInstance(VerifyFragment.FROM_PHONE, addPhoneSource = source), VerifyFragment.TAG)
                },
                onCustomerService = {
                    AnalyticsTracker.trackCustomerServiceDialog(AnalyticsTracker.CustomerServiceSource.ADD_PHONE_NOTICE)
                }
            )
        }
    }
}
