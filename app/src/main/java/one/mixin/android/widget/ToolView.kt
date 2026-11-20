package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ViewToolBinding
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isData
import one.mixin.android.vo.isText
import one.mixin.android.vo.supportSticker
import one.mixin.android.websocket.PinAction

class ToolView constructor(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    enum class Action(@StringRes val title: Int, @DrawableRes val icon: Int) {
        REPLY(R.string.Reply, R.drawable.ic_reply),
        COPY(R.string.Copy, R.drawable.ic_content_copy),
        FORWARD(R.string.Forward, R.drawable.ic_msg_forward),
        PIN(R.string.pin_title, R.drawable.ic_message_pin),
        UNPIN(R.string.Unpin, R.drawable.ic_message_unpin),
        SHARE(R.string.Share, R.drawable.ic_share),
        ADD_STICKER(R.string.Add_Sticker, R.drawable.ic_sticker_add),
        DELETE(R.string.Delete, R.drawable.ic_msg_delete),
        SELECT(R.string.Select_Multiple_Messages, R.drawable.ic_check_black_24dp),
    }

    private val binding = ViewToolBinding.inflate(LayoutInflater.from(context), this, true)
    val closeIv = binding.closeIv
    var onAction: ((Action) -> Unit)? = null

    private val actionViews = mapOf(
        Action.REPLY to binding.replyIv,
        Action.COPY to binding.copyIv,
        Action.FORWARD to binding.forwardIv,
        Action.PIN to binding.pinIv,
        Action.SHARE to binding.shareIv,
        Action.ADD_STICKER to binding.addStickerIv,
        Action.DELETE to binding.deleteIv,
    )
    private var actions = emptyList<Action>()
    private var popupMenu: PopupMenu? = null
    private var multipleSelection = false
    private var selectionCount = 0

    init {
        binding.moreIv.setOnClickListener { showMenu(it) }
        binding.closeIv.tooltipText = context.getString(R.string.Close)
    }

    fun updateSelection(messages: Collection<MessageItem>, pinAction: PinAction? = null) {
        selectionCount = messages.size
        multipleSelection = messages.isNotEmpty() && (multipleSelection || messages.size > 1)
        actions = messageSelectionActions(messages, pinAction)
        updateToolbar()
        if (actions.isEmpty() || multipleSelection) {
            dismissMenu()
        } else {
            popupMenu?.menu?.let { menu ->
                Action.entries.forEach { action ->
                    menu.findItem(action.ordinal + 1).isVisible = action in actions
                }
            }
        }
    }

    fun enterMultipleSelection() {
        if (actions.isEmpty()) return
        multipleSelection = true
        dismissMenu()
        updateToolbar()
    }

    private fun updateToolbar() {
        binding.countTv.text = if (multipleSelection) {
            selectionCount.toString()
        } else {
            resources.getQuantityString(R.plurals.items_selected, selectionCount, selectionCount)
        }
        binding.closeIv.setImageResource(if (multipleSelection) R.drawable.ic_msg_close else R.drawable.ic_arrow_back)
        binding.moreIv.isVisible = !multipleSelection && actions.isNotEmpty()
        actionViews.forEach { (action, view) ->
            val currentAction = if (action == Action.PIN && Action.UNPIN in actions) Action.UNPIN else action
            view.isVisible = multipleSelection && currentAction in actions
            view.setImageResource(currentAction.icon)
            view.contentDescription = context.getString(currentAction.title)
            view.tooltipText = view.contentDescription
            view.setOnClickListener {
                if (currentAction in actions) onAction?.invoke(currentAction)
            }
        }
    }

    fun showMenu(anchor: View) {
        if (actions.isEmpty() || multipleSelection || !anchor.isAttachedToWindow) return
        dismissMenu()
        val popup = PopupMenu(context, anchor, Gravity.END, 0, R.style.MessageActionPopup)
        popup.setForceShowIcon(true)
        Action.entries.forEach { action ->
            popup.menu.add(0, action.ordinal + 1, action.ordinal, action.title).apply {
                setIcon(action.icon)
                isVisible = action in actions
            }
        }
        popup.setOnMenuItemClickListener { item ->
            val action = Action.entries.getOrNull(item.itemId - 1)
            if (action == null || action !in actions) {
                false
            } else {
                dismissMenu()
                onAction?.invoke(action)
                true
            }
        }
        popup.setOnDismissListener {
            if (popupMenu === it) popupMenu = null
        }
        popupMenu = popup
        popup.show()
    }

    fun dismissMenu() {
        popupMenu?.dismiss()
        popupMenu = null
    }

    override fun onDetachedFromWindow() {
        dismissMenu()
        super.onDetachedFromWindow()
    }
}

internal fun messageSelectionActions(
    messages: Collection<MessageItem>,
    pinAction: PinAction? = null,
): List<ToolView.Action> {
    if (messages.isEmpty()) return emptyList()
    val message = messages.singleOrNull()
    return buildList {
        if (message != null && !message.canNotReply()) add(ToolView.Action.REPLY)
        if (message?.isText() == true) add(ToolView.Action.COPY)
        if (messages.size <= 99 && messages.none { it.canNotForward() }) add(ToolView.Action.FORWARD)
        if (message != null && !message.canNotPin() && pinAction != null) {
            add(if (pinAction == PinAction.PIN) ToolView.Action.PIN else ToolView.Action.UNPIN)
        }
        if (message?.isData() == true) add(ToolView.Action.SHARE)
        if (message?.supportSticker() == true) add(ToolView.Action.ADD_STICKER)
        add(ToolView.Action.DELETE)
        if (message != null) add(ToolView.Action.SELECT)
    }
}
