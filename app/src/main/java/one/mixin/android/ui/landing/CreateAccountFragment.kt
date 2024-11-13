package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_LANDING_CREATE
import one.mixin.android.ui.landing.components.CreateAccountPage
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Hyperlink

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
        binding.compose.setContent {
            CreateAccountPage({
                activity?.addFragment(
                    this@CreateAccountFragment,
                    MobileFragment.newInstance(from = FROM_LANDING_CREATE),
                    MobileFragment.TAG,
                )
            },{
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
