package one.mixin.android.ui.common.bottom

import android.annotation.SuppressLint
import android.app.Dialog
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.updateLayoutParams
import kotlinx.android.synthetic.main.fragment_motion_bottom_sheet.view.*
import moe.feng.support.biometricprompt.dip
import one.mixin.android.R
import one.mixin.android.extension.round
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet

abstract class MotionBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    private val roundSize by lazy {
        requireContext().dip(16f)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_motion_bottom_sheet, null)
        contentView.motion.setDebugMode(MotionLayout.DEBUG_SHOW_PATH)
        contentView.motion.updateLayoutParams<FrameLayout.LayoutParams> {
            if (isAdded) {
                topMargin = requireContext().statusBarHeight()
            }
        }
        contentView.user_bottom_bg.round(roundSize)
        (dialog as BottomSheet).apply {
            fullScreen = true
            setCustomView(contentView)
        }
        contentView.user_bottom_empty.setOnClickListener {
            dismissAllowingStateLoss()
        }
        contentView.user_bottom_close.setOnClickListener {
            dismissAllowingStateLoss()
        }
        contentView.user_bottom_name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        contentView.user_bottom_number.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)

        contentView.motion.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
            }

            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
                contentView.user_bottom_bg.round((1 - progress) * roundSize)
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
            }
        })

        contentView.user_bottom_menu.addView(
            getBottomView(),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        initView(contentView)
    }

    abstract fun getBottomView(): View

    abstract fun initView(contentView: View)

}
