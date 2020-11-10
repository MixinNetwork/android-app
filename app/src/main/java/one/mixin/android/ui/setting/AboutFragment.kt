package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.openMarket
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.widget.DebugClickListener

@AndroidEntryPoint
class AboutFragment : BaseFragment() {
    companion object {
        const val TAG = "AboutFragment"

        fun newInstance() = AboutFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_about, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val versionName = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        title_view.setSubTitle(getString(R.string.app_name), getString(R.string.about_version, versionName))
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        imageView.setOnClickListener(object : DebugClickListener() {
            override fun onDebugClick() {
                if (defaultSharedPreferences.getBoolean(Constants.Debug.WEB_DEBUG, false)) {
                    defaultSharedPreferences.putBoolean(Constants.Debug.WEB_DEBUG, false)
                    toast(R.string.web_debug_disable)
                } else {
                    defaultSharedPreferences.putBoolean(Constants.Debug.WEB_DEBUG, true)
                    toast(R.string.web_debug_enable)
                }
            }

            override fun onSingleClick() {}
        })
        twitter.setOnClickListener { context?.openUrl("https://twitter.com/MixinMessenger") }
        facebook.setOnClickListener { context?.openUrl("https://fb.com/MixinMessenger") }
        help_center.setOnClickListener { context?.openUrl(Constants.HelpLink.CENTER) }
        terms.setOnClickListener { context?.openUrl(getString(R.string.landing_terms_url)) }
        privacy.setOnClickListener { context?.openUrl(getString(R.string.landing_privacy_policy_url)) }
        checkUpdates.setOnClickListener { context?.openMarket() }
    }
}
