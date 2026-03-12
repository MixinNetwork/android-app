package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.crypto.mnemonicChecksum
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.ui.web.WebFragment
import one.mixin.android.util.viewBinding
import timber.log.Timber

@AndroidEntryPoint
class LandingMnemonicPhraseFragment : BaseFragment(R.layout.fragment_landing_mnemonic_phrase) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"

        fun newInstance(
        ): LandingMnemonicPhraseFragment =
            LandingMnemonicPhraseFragment().apply {

            }
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (activity is LandingActivity) {
            applySafeTopPadding(view)
        }
        Timber.e("LandingMnemonicPhraseFragment onViewCreated")
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
        binding.titleView.setOnLongClickListener {
            LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
            true
        }
        binding.compose.setContent {
            MnemonicPhraseInput(MnemonicState.Input, onComplete = {
                val list = ArrayList<String>()
                list.addAll(it)
                if (mnemonicChecksum(list)) {
                    navTo(MnemonicPhraseFragment.newInstance(list), MnemonicPhraseFragment.TAG)
                } else {
                    toast(R.string.invalid_mnemonic_phrase)
                }
            }, onCreate = {
                CreateAccountConfirmBottomSheetDialogFragment.newInstance()
                    .setOnCreateAccount {
                        activity?.addFragment(
                            this@LandingMnemonicPhraseFragment,
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
            )
        }
    }

    private fun applySafeTopPadding(rootView: View) {
        val originalPaddingTop: Int = rootView.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v: View, insets: WindowInsetsCompat ->
            val topInset: Int = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top
            v.setPadding(v.paddingLeft, originalPaddingTop + topInset, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
    }
}