package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAboutBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openMarket
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.DebugClickListener

@AndroidEntryPoint
class AboutFragment : BaseFragment(R.layout.fragment_about) {
    companion object {
        const val TAG = "AboutFragment"

        fun newInstance() = AboutFragment()
    }

    private val binding by viewBinding(FragmentAboutBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.setSubTitle(
                getString(R.string.app_name),
                "${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}",
            )
            titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            logAndDebug.isVisible = defaultSharedPreferences.getBoolean(Constants.Debug.LOG_AND_DEBUG, false)
            imageView.setOnClickListener(
                object : DebugClickListener() {
                    override fun onDebugClick() {
                        if (defaultSharedPreferences.getBoolean(Constants.Debug.LOG_AND_DEBUG, false)) {
                            defaultSharedPreferences.putBoolean(Constants.Debug.LOG_AND_DEBUG, false)
                            logAndDebug.isVisible = false
                        } else {
                            defaultSharedPreferences.putBoolean(Constants.Debug.LOG_AND_DEBUG, true)
                            logAndDebug.isVisible = true
                        }
                    }

                    override fun onSingleClick() {}
                },
            )
            twitter.setOnClickListener { context?.openUrl("https://twitter.com/MixinMessenger") }
            facebook.setOnClickListener { context?.openUrl("https://fb.com/MixinMessenger") }
            helpCenter.setOnClickListener { context?.openUrl(Constants.HelpLink.CENTER) }
            terms.setOnClickListener { context?.openUrl(getString(R.string.landing_terms_url)) }
            privacy.setOnClickListener { context?.openUrl(getString(R.string.landing_privacy_policy_url)) }
            checkUpdates.setOnClickListener { context?.openMarket(parentFragmentManager, lifecycleScope) }
            logAndDebug.setOnClickListener {
                navTo(LogAndDebugFragment.newInstance(), LogAndDebugFragment.TAG)
            }
        }
    }
}
