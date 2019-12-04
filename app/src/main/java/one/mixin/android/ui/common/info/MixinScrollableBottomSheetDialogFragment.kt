package one.mixin.android.ui.common.info

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uber.autodispose.android.lifecycle.scope
import javax.inject.Inject
import one.mixin.android.R
import one.mixin.android.di.Injectable
import one.mixin.android.ui.common.BottomSheetViewModel

abstract class MixinScrollableBottomSheetDialogFragment : BottomSheetDialogFragment(), Injectable {

    protected lateinit var contentView: View

    protected val stopScope = scope(Lifecycle.Event.ON_STOP)

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    protected val bottomViewModel: BottomSheetViewModel by viewModels { viewModelFactory }

    override fun getTheme() = R.style.AppTheme_Dialog

    protected var behavior: BottomSheetBehavior<*>? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, getLayoutId(), null)
        dialog.setContentView(contentView)
        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        behavior = params.behavior as? BottomSheetBehavior<*>
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            val defaultPeekHeight = getPeekHeight(contentView, behavior!!)
            behavior?.peekHeight = if (defaultPeekHeight == 0) {
                val scrollContent = contentView.findViewById<View>(R.id.scroll_content)
                val titleView = contentView.findViewById<View>(R.id.title)
                scrollContent.measure(
                    View.MeasureSpec.makeMeasureSpec(contentView.width, View.MeasureSpec.EXACTLY),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                titleView.measure(
                    View.MeasureSpec.makeMeasureSpec(contentView.width, View.MeasureSpec.EXACTLY),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                scrollContent.measuredHeight + titleView.measuredHeight
            } else defaultPeekHeight
            behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
    }

    abstract fun getLayoutId(): Int

    open fun getPeekHeight(contentView: View, behavior: BottomSheetBehavior<*>): Int = 0

    open fun onStateChanged(bottomSheet: View, newState: Int) {}
    open fun onSlide(bottomSheet: View, slideOffset: Float) {}

    private val bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            this@MixinScrollableBottomSheetDialogFragment.onStateChanged(bottomSheet, newState)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            this@MixinScrollableBottomSheetDialogFragment.onSlide(bottomSheet, slideOffset)
        }
    }
}
