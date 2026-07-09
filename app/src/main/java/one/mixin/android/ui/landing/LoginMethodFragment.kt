package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentLoginMethodBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.openCustomerService
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_LANDING
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class LoginMethodFragment : BaseFragment(R.layout.fragment_login_method) {
    companion object {
        const val TAG: String = "LoginMethodFragment"

        fun newInstance() = LoginMethodFragment()
    }

    private val binding by viewBinding(FragmentLoginMethodBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (activity is LandingActivity) {
            applySafeTopPadding(view)
        }
        binding.titleView.rightIb.setImageResource(R.drawable.ic_support)
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightAnimator.displayedChild = 0
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.titleView.rightIb.setOnClickListener {
            openCustomerService(source = AnalyticsTracker.CustomerServiceSource.LOGIN_BY)
        }
        binding.titleView.setOnLongClickListener {
            LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
            true
        }
        binding.otherWalletsRow.setOnClickListener {
            activity?.addFragment(
                this,
                LandingMnemonicPhraseFragment.newInstance(LoginMnemonicMode.TWELVE_OR_TWENTY_FOUR),
                LandingMnemonicPhraseFragment.TAG,
            )
        }
        binding.mobileRow.setOnClickListener {
            activity?.addFragment(
                this,
                MobileFragment.newInstance(from = FROM_LANDING),
                MobileFragment.TAG,
            )
        }
        binding.recoveryKitRow.setOnClickListener {
            activity?.addFragment(
                this,
                LandingMnemonicPhraseFragment.newInstance(LoginMnemonicMode.THIRTEEN_OR_TWENTY_FIVE),
                LandingMnemonicPhraseFragment.TAG,
            )
        }
        binding.noAccount.setOnClickListener {
            AnalyticsTracker.trackSignUpStart(AnalyticsTracker.SignUpStartSource.LOGIN_START)
            CreateAccountConfirmBottomSheetDialogFragment.newInstance()
                .setOnCreateAccount {
                    activity?.addFragment(
                        this,
                        MnemonicPhraseFragment.newInstance(),
                        MnemonicPhraseFragment.TAG,
                    )
                }
                .setOnPrivacyPolicy {
                    activity?.openUrl(getString(R.string.landing_privacy_policy_url))
                }
                .setOnTermsOfService {
                    activity?.openUrl(getString(R.string.landing_terms_url))
                }
                .showNow(parentFragmentManager, CreateAccountConfirmBottomSheetDialogFragment.TAG)
        }

        val policy: String = requireContext().getString(R.string.Privacy_Policy)
        val termsService: String = requireContext().getString(R.string.Terms_of_Service)
        val policyWrapper = requireContext().getString(R.string.landing_introduction, "**$policy**", "**$termsService**")
        val policyUrl = getString(R.string.landing_privacy_policy_url)
        val termsUrl = getString(R.string.landing_terms_url)
        binding.introductionTv.highlightStarTag(
            policyWrapper,
            arrayOf(policyUrl, termsUrl),
        )
    }

    private fun applySafeTopPadding(rootView: View) {
        val originalPaddingTop: Int = rootView.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, insets: WindowInsetsCompat ->
            val topInset: Int = maxOf(
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top,
                insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top,
            )
            v.setPadding(v.paddingLeft, originalPaddingTop + topInset, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
    }
}
