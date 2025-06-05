package one.mixin.android.ui.setting.member

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.SchemeBottomSheet
import one.mixin.android.ui.conversation.link.parser.NewSchemeParser
import one.mixin.android.ui.setting.ui.page.MixinMemberUpgradePage
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.Plan
import timber.log.Timber
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class MixinMemberUpgradeBottomSheetDialogFragment : SchemeBottomSheet() {
    companion object {
        const val TAG = "MixinMemberUpgradeBottomSheetDialogFragment"

        fun newInstance() = MixinMemberUpgradeBottomSheetDialogFragment()
    }

    private var behavior: BottomSheetBehavior<*>? = null

    private val newSchemeParser: NewSchemeParser by lazy { NewSchemeParser(this, linkViewModel) }

    @Inject
    lateinit var jobManager: MixinJobManager

    val linkViewModel by viewModels<BottomSheetViewModel>()
    private val memberViewModel by viewModels<MemberViewModel>()

    override fun getTheme() = R.style.AppTheme_Dialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val isGoogleBillingReady by memberViewModel.isGoogleBillingReady.collectAsState(
                    initial = !BuildConfig.IS_GOOGLE_PLAY // If not Google Play build, assume Google billing is ready
                )

                MixinMemberUpgradePage(
                    currentUserPlan = Session.getAccount()!!.membership?.plan ?: Plan.None,
                    isBillingManagerInitialized = isGoogleBillingReady,
                    onClose = { dismiss() },
                    onUrlGenerated = { url ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            Timber.e("MixinMemberUpgradeBottomSheetDialogFragment url: $url")
                            WebActivity.show(requireContext(), url, null)
                        }
                    },
                    onGooglePlay = { orderId->
                        launchPurchase100Subscription(orderId)
                    }
                )
                doOnPreDraw {
                    val params = (it.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
                    behavior = params?.behavior as? BottomSheetBehavior<*>
                    behavior?.peekHeight =
                        requireContext().realSize().y - requireContext().statusBarHeight() - requireContext().navigationBarHeight()
                    behavior?.isDraggable = false
                    behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
                }
            }
        }
    }

    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, R.style.MixinBottomSheet)
        dialog.window?.let { window ->
            SystemUIManager.lightUI(window, requireContext().isNightMode())
        }
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> dismissAllowingStateLoss()
                    else -> {}
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {
            }
        }

    override fun showError(errorRes: Int) {
        super.showError(errorRes)
    }

    override fun showError(error: String) {
    }

    override fun syncUtxo() {
        jobManager.addJobInBackground(SyncOutputJob())
    }

    // Todo remove test code
    private fun launchPurchase100Subscription(orderId: String) {
        lifecycleScope.launch {
            memberViewModel.subscribe100(requireActivity(), orderId)
        }
    }
}
