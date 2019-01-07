package one.mixin.android.ui.panel

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.View
import one.mixin.android.R
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.PanelBarView
import one.mixin.android.widget.keyboard.InputAwareLayout

abstract class PanelBottomSheet : MixinBottomSheetDialogFragment() {

    protected var maxHeight = 0
    protected var closeHeight = 0
    protected var middleHeight = (maxHeight + closeHeight) / 2

    private val panelBarCallback = object : PanelBarView.Callback {
        override fun onDrag(dis: Float) {
            (dialog as BottomSheet).getCustomView()?.let {
                val height = it.layoutParams.height - dis.toInt()
                if (height in closeHeight..maxHeight) {
                    (dialog as BottomSheet).setCustomViewHeightSync(height)
                } else if (height < closeHeight) {
                    dismiss()
                }
            }
        }

        override fun onRelease() {
            (dialog as BottomSheet).getCustomView()?.let {
                val height = it.layoutParams.height
                when {
                    height < middleHeight -> {
                        (dialog as BottomSheet).setCustomViewHeight(0) {
                            dismiss()
                        }
                    }
                    else -> {
                        (dialog as BottomSheet).setCustomViewHeight(maxHeight)
                    }
                }
            }
        }

        override fun onClick() {
            dismiss()
        }

        override fun onTap() {
            onTapPanelBar()
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        InputAwareLayout.appreciable = false
    }

    override fun dismiss() {
        InputAwareLayout.appreciable = true
        super.dismiss()
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        maxHeight = context!!.displaySize().y - context!!.statusBarHeight()
        closeHeight = context!!.displaySize().y / 2
        middleHeight = (maxHeight + closeHeight) / 2
        contentView = View.inflate(context, getContentViewId(), null)
        val panelBar = contentView.findViewById<PanelBarView>(R.id.panel_bar)
        panelBar.maxDragDistance = (maxHeight - closeHeight).toFloat()
        panelBar.callback = panelBarCallback
        (dialog as BottomSheet).setCustomView(contentView)
    }

    protected open fun onTapPanelBar() {
        // Left empty for override
    }

    abstract fun getContentViewId(): Int
}