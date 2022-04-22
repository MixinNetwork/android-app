package one.mixin.android.ui.search

import android.content.Context
import android.view.View
import android.widget.PopupMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.isGroupConversation

class SearchChatPopupMenu(
    private val context: Context,
    private val lifecycleScope: CoroutineScope,
    private val searchViewModel: SearchViewModel,
    private val afterAction: () -> Unit,
) {
    fun showPopupMenu(chatMinimal: ChatMinimal, anchor: View) {
        val popupMenu = PopupMenu(context, anchor)
        popupMenu.inflate(R.menu.search_chat_menu)
        val muteItem = popupMenu.menu.findItem(R.id.mute)
        val isMute = chatMinimal.isMute()
        if (isMute) {
            muteItem.setTitle(R.string.Unmute)
        } else {
            muteItem.setTitle(R.string.Mute)
        }
        val hasPin = chatMinimal.pinTime != null
        val pinItem = popupMenu.menu.findItem(R.id.pin)
        if (hasPin) {
            pinItem.setTitle(R.string.Unpin)
        } else {
            pinItem.setTitle(R.string.Pin)
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.pin -> {
                    if (hasPin) {
                        searchViewModel.updateConversationPinTimeById(
                            chatMinimal.conversationId,
                            null,
                            null,
                            afterAction,
                        )
                        toast(R.string.Chat_unpinned)
                    } else {
                        searchViewModel.updateConversationPinTimeById(
                            chatMinimal.conversationId,
                            null,
                            nowInUtc(),
                            afterAction
                        )
                        toast(R.string.Chat_pinned)
                    }
                    popupMenu.dismiss()
                    afterAction.invoke()
                }
                R.id.mute -> {
                    if (isMute) {
                        unMute(chatMinimal)
                    } else {
                        showMuteDialog(chatMinimal)
                    }
                    popupMenu.dismiss()
                }
                R.id.delete -> {
                    context.alertDialogBuilder()
                        .setTitle(
                            context.getString(
                                R.string.conversation_delete_title,
                                chatMinimal.getConversationName()
                            )
                        )
                        .setMessage(context.getString(R.string.conversation_delete_tip))
                        .setNegativeButton(R.string.Cancel) { dialog, _ ->
                            dialog.dismiss()
                            popupMenu.dismiss()
                        }
                        .setPositiveButton(R.string.Confirm) { dialog, _ ->
                            searchViewModel.deleteConversation(chatMinimal.conversationId, afterAction)
                            dialog.dismiss()
                            popupMenu.dismiss()
                        }
                        .show()
                }
            }
            return@setOnMenuItemClickListener true
        }
        popupMenu.show()
    }

    private fun showMuteDialog(chatMinimal: ChatMinimal) {
        val choices = arrayOf(
            context.getString(R.string.one_hour),
            context.getString(R.string.Eight_hours),
            context.getString(R.string.one_week),
            context.getString(R.string.one_year)
        )
        var duration = Constants.Mute.MUTE_8_HOURS
        var whichItem = 0
        context.alertDialogBuilder()
            .setTitle(context.getString(R.string.contact_mute_title))
            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.Confirm) { dialog, _ ->
                if (chatMinimal.isGroupConversation()) {
                    lifecycleScope.launch {
                        handleMixinResponse(
                            invokeNetwork = {
                                searchViewModel.mute(
                                    duration.toLong(),
                                    conversationId = chatMinimal.conversationId
                                )
                            },
                            successBlock = { response ->
                                searchViewModel.updateGroupMuteUntil(
                                    chatMinimal.conversationId,
                                    response.data!!.muteUntil
                                )
                                afterAction.invoke()
                                toast(context.getString(R.string.contact_mute_title) + " ${chatMinimal.groupName} " + choices[whichItem])
                            }
                        )
                    }
                } else {
                    val account = Session.getAccount()
                    account?.let {
                        lifecycleScope.launch {
                            handleMixinResponse(
                                invokeNetwork = {
                                    searchViewModel.mute(
                                        duration.toLong(),
                                        senderId = it.userId,
                                        recipientId = chatMinimal.userId
                                    )
                                },
                                successBlock = { response ->
                                    searchViewModel.updateMuteUntil(
                                        chatMinimal.userId,
                                        response.data!!.muteUntil
                                    )
                                    afterAction.invoke()
                                    toast(context.getString(R.string.contact_mute_title) + "  ${chatMinimal.fullName}  " + choices[whichItem])
                                }
                            )
                        }
                    }
                }
                dialog.dismiss()
            }
            .setSingleChoiceItems(choices, 0) { _, which ->
                whichItem = which
                when (which) {
                    0 -> duration = Constants.Mute.MUTE_1_HOUR
                    1 -> duration = Constants.Mute.MUTE_8_HOURS
                    2 -> duration = Constants.Mute.MUTE_1_WEEK
                    3 -> duration = Constants.Mute.MUTE_1_YEAR
                }
            }
            .show()
    }

    private fun unMute(chatMinimal: ChatMinimal) {
        if (chatMinimal.isGroupConversation()) {
            lifecycleScope.launch {
                handleMixinResponse(
                    invokeNetwork = {
                        searchViewModel.mute(0, conversationId = chatMinimal.conversationId)
                    },
                    successBlock = { response ->
                        searchViewModel.updateGroupMuteUntil(
                            chatMinimal.conversationId,
                            response.data!!.muteUntil
                        )
                        afterAction.invoke()
                        toast(context.getString(R.string.Unmute) + " ${chatMinimal.groupName}")
                    }
                )
            }
        } else {
            Session.getAccount()?.let {
                lifecycleScope.launch {
                    handleMixinResponse(
                        invokeNetwork = {
                            searchViewModel.mute(
                                0,
                                senderId = it.userId,
                                recipientId = chatMinimal.userId,
                            )
                        },
                        successBlock = { response ->
                            searchViewModel.updateMuteUntil(
                                chatMinimal.userId,
                                response.data!!.muteUntil
                            )
                            afterAction.invoke()
                            toast(context.getString(R.string.Unmute) + " ${chatMinimal.fullName}")
                        }
                    )
                }
            }
        }
    }
}
