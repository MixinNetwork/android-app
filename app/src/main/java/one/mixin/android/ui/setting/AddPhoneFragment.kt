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
import one.mixin.android.ui.setting.ui.page.AddPhonePage
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class AddPhoneFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "AddPhoneFragment"
        private const val ARGS_SOURCE = "args_source"

        fun newInstance(
            source: String = AnalyticsTracker.AddPhoneSource.SETTINGS,
        ): AddPhoneFragment =
            AddPhoneFragment().apply {
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
            AddPhonePage(
                Session.hasPhone(),
                {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                },
                {
                    navTo(AddPhoneBeforeFragment.newInstance(source), AddPhoneBeforeFragment.TAG)
                },
                onCustomerService = {
                    AnalyticsTracker.trackCustomerServiceDialog(AnalyticsTracker.CustomerServiceSource.ADD_PHONE)
                }
            )
        }
    }
}
