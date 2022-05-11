package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAboutBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openMarket
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.diagnosis.DiagnosisFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.DebugClickListener

@AndroidEntryPoint
class AboutFragment : BaseFragment(R.layout.fragment_about) {
    companion object {
        const val TAG = "AboutFragment"

        fun newInstance() = AboutFragment()
    }

    private val binding by viewBinding(FragmentAboutBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val versionName = requireContext().packageManager.getPackageInfo(
            requireContext().packageName,
            0
        ).versionName
        binding.apply {
            titleView.setSubTitle(
                getString(R.string.app_name),
                getString(R.string.about_version, versionName)
            )
            titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
            imageView.setOnClickListener(object : DebugClickListener() {
                override fun onDebugClick() {
                    if (defaultSharedPreferences.getBoolean(Constants.Debug.WEB_DEBUG, false)) {
                        defaultSharedPreferences.putBoolean(Constants.Debug.WEB_DEBUG, false)
                        toast(R.string.Disable_web_debug)
                    } else {
                        defaultSharedPreferences.putBoolean(Constants.Debug.WEB_DEBUG, true)
                        toast(R.string.Enable_web_debug)
                    }
                }

                override fun onSingleClick() {}
            })

            titleView.titleContainer.setOnClickListener(object : DebugClickListener() {
                override fun onDebugClick() {
                    if (defaultSharedPreferences.getBoolean(Constants.Debug.DB_DEBUG, false)) {
                        defaultSharedPreferences.putBoolean(Constants.Debug.DB_DEBUG, false)
                        defaultSharedPreferences.putBoolean(Constants.Debug.DB_DEBUG_WARNING, true)
                        database.isVisible = false
                        toast(R.string.Disable_db_debug)
                    } else {
                        defaultSharedPreferences.putBoolean(Constants.Debug.DB_DEBUG, true)
                        database.isVisible = true
                        toast(R.string.Enable_db_debug)
                    }
                }

                override fun onSingleClick() {
                    navTo(DiagnosisFragment.newInstance(), DiagnosisFragment.TAG)
                }
            })
            twitter.setOnClickListener { context?.openUrl("https://twitter.com/MixinMessenger") }
            facebook.setOnClickListener { context?.openUrl("https://fb.com/MixinMessenger") }
            helpCenter.setOnClickListener { context?.openUrl(Constants.HelpLink.CENTER) }
            terms.setOnClickListener { context?.openUrl(getString(R.string.landing_terms_url)) }
            privacy.setOnClickListener { context?.openUrl(getString(R.string.landing_privacy_policy_url)) }
            checkUpdates.setOnClickListener { context?.openMarket() }
            database.isVisible = defaultSharedPreferences.getBoolean(Constants.Debug.DB_DEBUG, false)
            database.setOnClickListener {
                navTo(
                    DatabaseDebugFragment.newInstance(),
                    DatabaseDebugFragment.TAG
                )
            }
        }
    }
}
