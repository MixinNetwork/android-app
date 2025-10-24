package one.mixin.android.ui.common

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import com.google.android.material.bottomsheet.BottomSheetBehavior
import one.mixin.android.R
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.getSafeAreaInsetsBottom
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.util.SystemUIManager
import javax.inject.Inject
import one.mixin.android.extension.dp as dip

abstract class MixinComposeBottomSheetDialogFragment : SchemeBottomSheet() {

    @Inject
    lateinit var jobManager: MixinJobManager

    protected var behavior: BottomSheetBehavior<*>? = null

    @Composable
    protected abstract fun ComposeContent()

    protected abstract fun getBottomSheetHeight(view: View): Int
    protected open fun onBottomSheetStateChanged(bottomSheet: View, newState: Int) {}
    protected open fun onBottomSheetSlide(bottomSheet: View, slideOffset: Float) {}

    private val internalBottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismissAllowingStateLoss()
            }
            onBottomSheetStateChanged(bottomSheet, newState)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            onBottomSheetSlide(bottomSheet, slideOffset)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val composeView = ComposeView(requireContext()).apply {
            roundTopOrBottom(11.dip.toFloat(), top = true, bottom = false)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComposeContent()
            }
        }
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.doOnPreDraw {
            val params = (view.parent as? View)?.layoutParams as? CoordinatorLayout.LayoutParams
            behavior = params?.behavior as? BottomSheetBehavior<*>
            behavior?.peekHeight = getBottomSheetHeight(view)
            behavior?.isDraggable = false
            behavior?.addBottomSheetCallback(internalBottomSheetBehaviorCallback)
            view.setPadding(0,0,0, view.getSafeAreaInsetsBottom())
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
            window.setGravity(Gravity.BOTTOM)
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    override fun syncUtxo() {
        jobManager.addJobInBackground(SyncOutputJob())
    }

}
