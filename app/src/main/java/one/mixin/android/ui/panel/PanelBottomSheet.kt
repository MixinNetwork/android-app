package one.mixin.android.ui.panel

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import one.mixin.android.R
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.PanelBarView
import one.mixin.android.widget.keyboard.InputAwareLayout

abstract class PanelBottomSheet : MixinBottomSheetDialogFragment() {

    private val maxHeight by lazy {
        context!!.displaySize().y - context!!.statusBarHeight()
    }
    private val closeHeight by lazy {
        context!!.displaySize().y / 2
    }
    private val middleHeight by lazy {
        (maxHeight + closeHeight) / 2
    }

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

        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        InputAwareLayout.appreciable = false
    }

    override fun onDetach() {
        super.onDetach()
        InputAwareLayout.appreciable = true
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, getContentViewId(), null)
        val panelBar = contentView.findViewById<PanelBarView>(R.id.panel_bar)
        panelBar.maxDragDistance = (maxHeight - closeHeight).toFloat()
        panelBar.callback = panelBarCallback
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (dialog as BottomSheet).setCustomViewHeight(maxHeight)
    }

    abstract fun getContentViewId(): Int
}