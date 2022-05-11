package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import one.mixin.android.R
import one.mixin.android.databinding.FragmentLandingBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.highlightLinkText
import one.mixin.android.extension.navTo
import one.mixin.android.ui.setting.diagnosis.DiagnosisFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.DebugClickListener

class LandingFragment : Fragment(R.layout.fragment_landing) {

    companion object {
        const val TAG: String = "LandingFragment"

        fun newInstance() = LandingFragment()
    }

    private val binding by viewBinding(FragmentLandingBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val policy: String = getString(R.string.Privacy_Policy)
        val termsService: String = getString(R.string.Terms_of_Service)
        val policyWrapper = getString(R.string.landing_introduction, policy, termsService)
        val policyUrl = getString(R.string.landing_privacy_policy_url)
        val termsUrl = getString(R.string.landing_terms_url)
        binding.introductionTv.highlightLinkText(
            policyWrapper,
            arrayOf(policy, termsService),
            arrayOf(policyUrl, termsUrl)
        )

        binding.agreeTv.setOnClickListener {
            activity?.addFragment(
                this@LandingFragment,
                MobileFragment.newInstance(),
                MobileFragment.TAG
            )
        }
        binding.imageView.setOnClickListener(object : DebugClickListener() {
            override fun onDebugClick() {
                navTo(DiagnosisFragment.newInstance(), DiagnosisFragment.TAG)
            }

            override fun onSingleClick() {
            }
        })
    }
}
