package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_LANDING
import one.mixin.android.ui.landing.components.CreateAccountPage
import one.mixin.android.ui.web.WebFragment
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding

class CreateAccountFragment : Fragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "LandingFragment"

        fun newInstance() = CreateAccountFragment()
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.setSubTitle(requireContext().getString(R.string.Create_Account), "")
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.titleView.rightIb.setImageResource(R.drawable.ic_support)
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightAnimator.displayedChild = 0
        binding.titleView.rightIb.setOnClickListener {
            val bundle = Bundle().apply {
                putString(WebFragment.URL, Constants.HelpLink.CUSTOMER_SERVICE)
                putBoolean(WebFragment.ARGS_INJECTABLE, false)
            }
            navTo(WebFragment.newInstance(bundle), WebFragment.TAG)
        }
        binding.compose.setContent {
            CreateAccountPage({ create ->
                if (create) {
                    AnalyticsTracker.trackSignUpStart("mobile_number")
                }
                activity?.addFragment(
                    this@CreateAccountFragment,
                    MobileFragment.newInstance(from = FROM_LANDING),
                    MobileFragment.TAG,
                )
            }, {
                AnalyticsTracker.trackSignUpStart("mnemonic_phrase")
                activity?.addFragment(
                    this@CreateAccountFragment,
                    MnemonicPhraseFragment.newInstance(),
                    MnemonicPhraseFragment.TAG,
                )
            }, {
                activity?.openUrl(Constants.HelpLink.TIP)
            }, {
                activity?.openUrl(getString(R.string.landing_privacy_policy_url))
            }, {
                activity?.openUrl(getString(R.string.landing_terms_url))
            }, {
                activity?.openUrl(getString(R.string.landing_privacy_policy_url))
            })
        }
    }
}
