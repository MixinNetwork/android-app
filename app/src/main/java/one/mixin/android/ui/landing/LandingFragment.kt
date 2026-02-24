package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.databinding.FragmentLandingBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.landing.MobileFragment.Companion.FROM_LANDING
import one.mixin.android.ui.setting.diagnosis.DiagnosisFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.DebugClickListener
import timber.log.Timber

class LandingFragment : Fragment(R.layout.fragment_landing) {
    companion object {
        const val TAG: String = "LandingFragment"

        fun newInstance() = LandingFragment()
    }

    private val binding by viewBinding(FragmentLandingBinding::bind)

    private var featureCount: Int = 0
    private var lastUserInteractionTime: Long = 0L

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        SystemUIManager.setSafePadding(requireActivity().window, requireContext().colorFromAttribute(R.attr.bg_white), onlyNav = true, imePadding = false)
        Timber.e("\n-----------------------------------")
        Timber.e("MobileFragment onViewCreated")
        val features: List<LandingFeatureItem> = listOf(
            LandingFeatureItem(
                imageResId = R.drawable.ic_landing_carousel_mixin,
                title = getString(R.string.Mixin),
                description = getString(R.string.onboarding_mixin_description),
            ),
            LandingFeatureItem(
                imageResId = R.drawable.ic_landing_carousel_non_custodial,
                title = getString(R.string.onboarding_decentralized_title),
                description = getString(R.string.onboarding_decentralized_description),
            ),
            LandingFeatureItem(
                imageResId = R.drawable.ic_landing_carousel_trade,
                title = getString(R.string.onboarding_trade_title),
                description = getString(R.string.onboarding_trade_description),
            ),
            LandingFeatureItem(
                imageResId = R.drawable.ic_landing_carousel_private_chat,
                title = getString(R.string.onboarding_privacy_title),
                description = getString(R.string.onboarding_privacy_description),
            ),
            LandingFeatureItem(
                imageResId = R.drawable.ic_landing_carousel_recovery,
                title = getString(R.string.onboarding_recover_title),
                description = getString(R.string.onboarding_recover_description),
            ),
            LandingFeatureItem(
                imageResId = R.drawable.ic_landing_carousel_rewards,
                title = getString(R.string.onboarding_reward_title),
                description = getString(R.string.onboarding_reward_description),
            ),
        )
        featureCount = features.size
        binding.featurePager.adapter = LandingFeatureAdapter(features)
        binding.featurePager.offscreenPageLimit = features.size
        
        val screenHeightPx = binding.root.resources.displayMetrics.heightPixels
        val targetHeightDp = 156f
        val targetHeightPx = targetHeightDp * resources.displayMetrics.density
        val calculatedPercent = targetHeightPx / screenHeightPx
        val finalPercent = 0.4f + calculatedPercent
        
        (binding.featurePager.layoutParams as ConstraintLayout.LayoutParams).apply {
            matchConstraintPercentHeight = finalPercent
            height = 0
        }
        binding.featurePager.requestLayout()
        
        binding.featurePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    lastUserInteractionTime = System.currentTimeMillis()
                }
            }
        })
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastInteraction = currentTime - lastUserInteractionTime
                    
                    if (timeSinceLastInteraction < 3000L) {
                        delay(3000L - timeSinceLastInteraction)
                        continue
                    }
                    
                    delay(3000L)
                    if (featureCount <= 1) {
                        continue
                    }
                    val nextItem = (binding.featurePager.currentItem + 1) % featureCount
                    binding.featurePager.setCurrentItem(nextItem, true)
                }
            }
        }
        val mediator = TabLayoutMediator(binding.featureIndicator, binding.featurePager) { tab, _ ->
            val dotView: View = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.landing_indicator_dot_size),
                    resources.getDimensionPixelSize(R.dimen.landing_indicator_dot_size),
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.landing_indicator_dot_spacing)
                    marginStart = resources.getDimensionPixelSize(R.dimen.landing_indicator_dot_spacing)
                }
                setBackgroundResource(R.drawable.bg_landing_indicator_dot)
                isSelected = false
            }
            tab.customView = dotView
        }
        mediator.attach()
        binding.featureIndicator.getTabAt(binding.featurePager.currentItem)?.customView?.isSelected = true
        binding.featureIndicator.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                    tab.customView?.isSelected = true
                }

                override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                    tab.customView?.isSelected = false
                }

                override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                }
            }
        )

        binding.featurePager.setOnClickListener(
            object : DebugClickListener() {
                override fun onDebugClick() {
                    navTo(DiagnosisFragment.newInstance(), DiagnosisFragment.TAG)
                }

                override fun onSingleClick() {
                }
            },
        )

        binding.version.text = getString(R.string.current_version, BuildConfig.VERSION_NAME)
        binding.createTv.setOnClickListener {
            CreateAccountConfirmBottomSheetDialogFragment.newInstance()
                .setOnCreateAccount {
                    activity?.addFragment(
                        this@LandingFragment,
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
        binding.continueTv.setOnClickListener {
            AnalyticsTracker.trackLoginStart()
            activity?.addFragment(
                this@LandingFragment,
                MobileFragment.newInstance(from = FROM_LANDING),
                MobileFragment.TAG,
            )
        }
    }
}
