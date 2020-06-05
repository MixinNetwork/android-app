package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_landing.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.highlightLinkText

class LandingFragment : Fragment() {

    companion object {
        const val TAG: String = "LandingFragment"

        fun newInstance() = LandingFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_landing, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val policy: String = getString(R.string.landing_privacy_policy)
        val termsService: String = getString(R.string.landing_terms_service)
        val policyWrapper = getString(R.string.landing_introduction, policy, termsService)
        val policyUrl = getString(R.string.landing_privacy_policy_url)
        val termsUrl = getString(R.string.landing_terms_url)
        introduction_tv.highlightLinkText(
            policyWrapper,
            arrayOf(policy, termsService),
            arrayOf(policyUrl, termsUrl)
        )

        agree_tv.setOnClickListener {
            activity?.addFragment(this@LandingFragment, MobileFragment.newInstance(), MobileFragment.TAG)
        }
    }
}
