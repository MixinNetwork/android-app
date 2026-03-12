package one.mixin.android.ui.common.info

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uber.autodispose.android.lifecycle.scope
import one.mixin.android.Constants.Colors.LINK_COLOR
import one.mixin.android.R
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.resolveBottomSheetPeekHeight
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.SystemUIManager
import one.mixin.android.widget.MixinBottomSheetDialog
import one.mixin.android.widget.linktext.AutoLinkMode
import one.mixin.android.widget.linktext.AutoLinkTextView
import timber.log.Timber

abstract class MixinScrollableBottomSheetDialogFragment : BottomSheetDialogFragment() {
    protected lateinit var contentView: View

    protected val stopScope = scope(Lifecycle.Event.ON_STOP)

    protected val pauseScope = scope(Lifecycle.Event.ON_PAUSE)

    protected val bottomViewModel by viewModels<BottomSheetViewModel>()

    override fun getTheme() = R.style.MixinBottomSheet

    protected var behavior: BottomSheetBehavior<*>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MixinBottomSheetDialog(requireContext(), theme).apply {
            dismissWithAnimation = true
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, getLayoutId(), null)
        dialog.setContentView(contentView)
        val params = (contentView.parent as View).layoutParams as? CoordinatorLayout.LayoutParams
        val bottomSheetBehavior = params?.behavior as? BottomSheetBehavior<*>
        behavior = bottomSheetBehavior
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.addBottomSheetCallback(bottomSheetBehaviorCallback)
            contentView.doOnPreDraw {
                val defaultPeekHeight = getPeekHeight(contentView, bottomSheetBehavior)
                val contentHeight =
                    if (defaultPeekHeight == 0) {
                        calculateMeasuredContentHeight(contentView)
                    } else {
                        defaultPeekHeight
                    }
                bottomSheetBehavior.peekHeight = requireContext().resolveBottomSheetPeekHeight(contentView, contentHeight)
                bottomSheetBehavior.isGestureInsetBottomIgnored = true
            }
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
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

    override fun onDetach() {
        super.onDetach()
        // UrlInterpreterActivity doesn't have a UI and needs it's son fragment to handle it's finish.
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                realFragmentCount++
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }

    override fun dismiss() {
        dismissAllowingStateLoss()
    }

    override fun dismissAllowingStateLoss() {
        try {
            super.dismissAllowingStateLoss()
        } catch (e: IllegalStateException) {
            Timber.e(e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    protected fun setDetailsTv(
        detailsTv: AutoLinkTextView,
        scrollView: NestedScrollView,
        conversationId: String?,
    ) {
        detailsTv.movementMethod = LinkMovementMethod()
        detailsTv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        detailsTv.setUrlModeColor(LINK_COLOR)
        detailsTv.setAutoLinkOnClickListener { _, url ->
            url.openAsUrlOrWeb(requireContext(), conversationId, parentFragmentManager, lifecycleScope)
            dismiss()
        }
        detailsTv.setOnTouchListener { _, _ ->
            if (detailsTv.canScrollVertically(1) ||
                detailsTv.canScrollVertically(-1)
            ) {
                detailsTv.parent.requestDisallowInterceptTouchEvent(true)
            }
            return@setOnTouchListener false
        }
        scrollView.setOnTouchListener { _, _ ->
            detailsTv.parent.requestDisallowInterceptTouchEvent(false)
            return@setOnTouchListener false
        }
    }

    abstract fun getLayoutId(): Int

    open fun getPeekHeight(
        contentView: View,
        behavior: BottomSheetBehavior<*>,
    ): Int = 0

    open fun onStateChanged(
        bottomSheet: View,
        newState: Int,
    ) {}

    open fun onSlide(
        bottomSheet: View,
        slideOffset: Float,
    ) {}

    private fun calculateMeasuredContentHeight(contentView: View): Int {
        val scrollContent = contentView.findViewById<View>(R.id.scroll_content)
        val titleView = contentView.findViewById<View>(R.id.title)
        val width = contentView.measuredWidth.takeIf { it > 0 } ?: contentView.width
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)

        scrollContent.measure(
            widthSpec,
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        titleView.measure(
            widthSpec,
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        return scrollContent.measuredHeight + titleView.measuredHeight
    }

    private val bottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                this@MixinScrollableBottomSheetDialogFragment.onStateChanged(bottomSheet, newState)
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {
                this@MixinScrollableBottomSheetDialogFragment.onSlide(bottomSheet, slideOffset)
            }
        }
}
