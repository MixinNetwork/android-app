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
import androidx.collection.arraySetOf
import androidx.core.widget.PopupWindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.databinding.ViewToolBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.fadeOut
import one.mixin.android.session.Session
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.isData
import one.mixin.android.vo.isText
import one.mixin.android.vo.supportSticker
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

    // Select message logic
    private val selectSet = arraySetOf<MessageItem>()

    fun selectItem() = selectSet

    fun isSelect(messageId: String?): Boolean {
        return if (selectSet.isEmpty()) {
            false
        } else {
            selectSet.find { it.messageId == messageId } != null
        }
    }

    fun hasSelect() = selectSet.isNotEmpty()

    fun addSelect(messageItem: MessageItem): Boolean {
        return selectSet.add(messageItem).also {
            update()
        }
    }

    fun removeSelect(messageItem: MessageItem): Boolean {
        return selectSet.remove(selectSet.find { it.messageId == messageItem.messageId }).also {
            update()
        }
    }

    fun firstItem(): MessageItem? = selectSet.firstOrNull()

    fun clear() = selectSet.clear()

    fun <R>mapItem(transform: (MessageItem) -> R) = selectSet.map(transform)

    private fun update(){
        countTv.text = selectSet.size.toString()
        when {
            !hasSelect() -> fadeOut()
            selectSet.size == 1 -> {
                try {
                    if (firstItem()?.isText() == true) {
                        copyIv.visibility = VISIBLE
                    } else {
                        copyIv.visibility = GONE
                    }
                } catch (e: ArrayIndexOutOfBoundsException) {
                    copyIv.visibility = GONE
                }
                if (firstItem()?.isData() == true) {
                    shareIv.visibility = VISIBLE
                } else {
                    shareIv.visibility = GONE
                }
                if (firstItem()?.supportSticker() == true) {
                    addStickerIv.visibility = VISIBLE
                } else {
                    addStickerIv.visibility = GONE
                }
                if (firstItem()?.canNotReply() == true) {
                    replyIv.visibility = GONE
                } else {
                    replyIv.visibility = VISIBLE
                }
                checkPinMessage()
            }
            else -> {
                forwardIv.visibility = VISIBLE
                replyIv.visibility = GONE
                copyIv.visibility = GONE
                addStickerIv.visibility = GONE
                shareIv.visibility = GONE
                pinIv.visibility = GONE
            }
        }
        if (selectSet.size > 99 || selectSet.any { it.canNotForward() }) {
            forwardIv.visibility = GONE
        } else {
            forwardIv.visibility = VISIBLE
        }
    }

    private fun checkPinMessage() {
        if (firstItem()?.canNotPin() == true) {
            pinIv.visibility = GONE
        } else {
            firstItem()?.messageId?.let { messageId ->
            // Todo
            //     lifecycleScope.launch {
            //         if (isGroup) {
            //             val role = withContext(Dispatchers.IO) {
            //                 chatViewModel.findParticipantById(
            //                     conversationId,
            //                     Session.getAccountId()!!,
            //                 )?.role
            //             }
            //             if (role != ParticipantRole.OWNER.name && role != ParticipantRole.ADMIN.name) {
            //                 pinIv.visibility = GONE
            //                 return@launch
            //             }
            //         }
            //         val pinMessage = chatViewModel.findPinMessageById(messageId)
            //         if (pinMessage == null) {
            //             pinIv.tag = PinAction.PIN
            //             pinIv.setImageResource(R.drawable.ic_message_pin)
            //             pinIv.visibility = VISIBLE
            //         } else {
            //             pinIv.tag = PinAction.UNPIN
            //             pinIv.setImageResource(R.drawable.ic_message_unpin)
            //             pinIv.visibility = VISIBLE
            //         }
            //     }
            }
        }
    }


    init {
        closeIv.setOnLongClickListener {
            showTip(it, R.string.Close)
            true
        }
        copyIv.setOnLongClickListener {
            showTip(it, R.string.Copy)
            true
        }
        deleteIv.setOnLongClickListener {
            showTip(it, R.string.Delete)
            true
        }
        addStickerIv.setOnLongClickListener {
            showTip(it, R.string.Add_Sticker)
            true
        }
        forwardIv.setOnLongClickListener {
            showTip(it, R.string.Forward)
            true
        }
        replyIv.setOnLongClickListener {
            showTip(it, R.string.Reply)
            true
        }
        shareIv.setOnLongClickListener {
            showTip(it, R.string.Share)
            true
        }
        pinIv.setOnLongClickListener {
            showTip(
                it,
                if (it.tag == PinAction.PIN) {
                    R.string.pin_title
                } else {
                    R.string.Unpin
                },
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
