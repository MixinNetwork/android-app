package one.mixin.android.ui.landing

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_landing.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.widget.NoUnderLineSpan

class LandingFragment : Fragment() {

    companion object {
        val TAG: String = "LandingFragment"

        fun newInstance() = LandingFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_landing, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val policy: String = getString(R.string.landing_privacy_policy)
        val termsService: String = getString(R.string.landing_terms_service)
        val policyWrapper = getString(R.string.landing_introduction, policy, termsService)
        val colorPrimary = ContextCompat.getColor(context!!, R.color.colorBlue)
        val policyUrl = getString(R.string.landing_privacy_policy_url)
        val termsUrl = getString(R.string.landing_terms_url)
        introduction_tv.text = highlightLinkText(
            policyWrapper,
            colorPrimary,
            arrayOf(policy, termsService),
            arrayOf(policyUrl, termsUrl))
        introduction_tv.movementMethod = LinkMovementMethod.getInstance()

        agree_tv.setOnClickListener {
            activity?.addFragment(this@LandingFragment, MobileFragment.newInstance(), MobileFragment.TAG)
        }
    }

    private fun highlightLinkText(
        source: String,
        color: Int,
        texts: Array<String>,
        links: Array<String>
    ): SpannableString {
        if (texts.size != links.size) {
            throw IllegalArgumentException("texts's length should equals with links")
        }
        val sp = SpannableString(source)
        for (i in texts.indices) {
            val text = texts[i]
            val link = links[i]
            val start = source.indexOf(text)
            sp.setSpan(NoUnderLineSpan(link), start, start + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(ForegroundColorSpan(color), start, start + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sp
    }
}