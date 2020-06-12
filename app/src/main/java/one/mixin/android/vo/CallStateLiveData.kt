package one.mixin.android.vo

import android.content.Context
import androidx.lifecycle.LiveData
import one.mixin.android.webrtc.CallService
import one.mixin.android.webrtc.cancelCall
import one.mixin.android.webrtc.declineCall
import one.mixin.android.webrtc.krakenCancel
import one.mixin.android.webrtc.krakenDecline
import one.mixin.android.webrtc.krakenEnd
import one.mixin.android.webrtc.localEnd
import timber.log.Timber

data class GroupCallState(
    var conversationId: String
) {
    var users: ArrayList<String>? = null
}

class CallStateLiveData : LiveData<CallService.CallState>() {
    var state: CallService.CallState = CallService.CallState.STATE_IDLE
        set(value) {
            if (field == value) return

            field = value
            postValue(value)
        }
    var conversationId: String? = null
    var trackId: String? = null
    var user: User? = null
    var connectedTime: Long? = null
    var isOffer: Boolean = true

    private val pendingGroupCalls = mutableSetOf<GroupCallState>()

    fun reset() {
        conversationId = null
        trackId = null
        user = null
        connectedTime = null
        isOffer = true
        state = CallService.CallState.STATE_IDLE
    }

    fun isGroupCall() = user == null

    fun addPendingGroupCall(conversationId: String) {
        val exists = pendingGroupCalls.find {
            it.conversationId == conversationId
        }
        if (exists != null) return

        pendingGroupCalls.add(GroupCallState(conversationId))
    }

    fun removePendingGroupCall(conversationId: String): Boolean {
        val each = pendingGroupCalls.iterator()
        var removed = false
        while (each.hasNext()) {
            if (conversationId == each.next().conversationId) {
                each.remove()
                removed = true
            }
        }
        Timber.d("removePendingGroupCall $removed")
        postValue(state)
        return removed
    }

    fun getUserByConversationId(conversationId: String) =
        pendingGroupCalls.find {
            it.conversationId == conversationId
        }?.users

    fun getUserCountByConversationId(conversationId: String): Int =
        getUserByConversationId(conversationId)?.size ?: 0

    fun setUsersByConversationId(conversationId: String, newUsers: ArrayList<String>?) {
        if (newUsers.isNullOrEmpty()) return

        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return
        groupCallState.users = newUsers

        postValue(state)
    }

    fun addUser(userId: String, conversationId: String) {
        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return
        groupCallState.users = addUserToList(userId, groupCallState.users)

        postValue(state)
    }

    fun removeUser(userId: String, conversationId: String) {
        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return
        groupCallState.users?.remove(userId)

        postValue(state)
    }

    fun isIdle() = state == CallService.CallState.STATE_IDLE
    fun isNotIdle() = state != CallService.CallState.STATE_IDLE
    fun isConnected() = state == CallService.CallState.STATE_CONNECTED
    fun isNotConnected() = state != CallService.CallState.STATE_CONNECTED

    fun isPendingGroupCall(conversationId: String) =
        if (this.conversationId == conversationId) {
            isIdle()
        } else {
            pendingGroupCalls.any { it.conversationId == conversationId }
        }

    fun handleHangup(ctx: Context) {
        when (state) {
            CallService.CallState.STATE_DIALING ->
                if (isGroupCall()) {
                    krakenCancel(ctx)
                } else {
                    cancelCall(ctx)
                }
            CallService.CallState.STATE_RINGING ->
                if (isGroupCall()) {
                    krakenDecline(ctx)
                } else {
                    declineCall(ctx)
                }
            CallService.CallState.STATE_ANSWERING -> {
                if (isOffer) {
                    cancelCall(ctx)
                } else {
                    declineCall(ctx)
                }
            }
            CallService.CallState.STATE_CONNECTED ->
                if (isGroupCall()) {
                    krakenEnd(ctx)
                } else {
                    localEnd(ctx)
                }
            else -> cancelCall(ctx)
        }
    }

    private fun addUserToList(userId: String, users: ArrayList<String>? = null): ArrayList<String> {
        val userList = users ?: arrayListOf()
        userList.let { us ->
            val existsUser = us.find { u -> u == userId }
            if (existsUser == null) {
                us.add(userId)
            }
        }
        return userList
    }
}
