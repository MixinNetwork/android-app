package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.widget.PopupWindowCompat
import one.mixin.android.R
import one.mixin.android.databinding.ViewToolBinding
import one.mixin.android.extension.dp
import one.mixin.android.websocket.PinAction

class ToolView constructor(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    private val binding = ViewToolBinding.inflate(LayoutInflater.from(context), this, true)
    val closeIv = binding.closeIv
    val copyIv = binding.copyIv
    val countTv = binding.countTv
    val deleteIv = binding.deleteIv
    val addStickerIv = binding.addStickerIv
    val replyIv = binding.replyIv
    val forwardIv = binding.forwardIv
    val shareIv = binding.shareIv
    val pinIv = binding.pinIv

    init {
        closeIv.setOnLongClickListener {
            showTip(it, R.string.action_close)
            true
        }
        copyIv.setOnLongClickListener {
            showTip(it, R.string.action_copy)
            true
        }
        deleteIv.setOnLongClickListener {
            showTip(it, R.string.action_delete)
            true
        }
        addStickerIv.setOnLongClickListener {
            showTip(it, R.string.add_sticker)
            true
        }
        forwardIv.setOnLongClickListener {
            showTip(it, R.string.action_forward)
            true
        }
        replyIv.setOnLongClickListener {
            showTip(it, R.string.action_reply)
            true
        }
        shareIv.setOnLongClickListener {
            showTip(it, R.string.action_share)
            true
        }
        pinIv.setOnLongClickListener {
            showTip(
                it,
                if (it.tag == PinAction.PIN) {
                    R.string.action_pin
                } else {
                    R.string.action_unpin
                }
            )
            true
        }
    }

    private val tipView by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_tip, null, false)
    }
    private val popupWindow by lazy {
        PopupWindow(context).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            contentView = tipView
        }
    }

    private fun showTip(view: View, @StringRes str: Int) {
        tipView.apply {
            (this as TextView).setText(str)
            setOnClickListener {
                popupWindow.dismiss()
            }
        }
        PopupWindowCompat.showAsDropDown(popupWindow, view, 0, 12.dp, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
    }
}
