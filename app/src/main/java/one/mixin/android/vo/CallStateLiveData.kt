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
    var users: ArrayList<String>? = null
    var connectedTime: Long? = null
    var isOffer: Boolean = true

    private val pendingGroupCalls = mutableSetOf<GroupCallState>()

    fun reset() {
        conversationId = null
        trackId = null
        user = null
        users = null
        connectedTime = null
        isOffer = true
        state = CallService.CallState.STATE_IDLE
    }

    fun isGroupCall() = user == null

    fun addPendingGroupCall(conversationId: String) {
        pendingGroupCalls.add(GroupCallState(conversationId))
        postValue(state)
    }

    fun getUserByConversationId(conversationId: String) =
        if (this.conversationId == conversationId) {
            users
        } else {
            pendingGroupCalls.find {
                it.conversationId == conversationId
            }?.users
        }

    fun getUserCountByConversationId(conversationId: String): Int =
        getUserByConversationId(conversationId)?.size ?: 0

    fun setUsersByConversationId(conversationId: String, newUsers: ArrayList<String>) {
        if (this.conversationId == conversationId) {
            users = newUsers
        } else {
            val groupCallState = pendingGroupCalls.find {
                it.conversationId == conversationId
            } ?: return
            groupCallState.users = newUsers
        }
    }

    fun addUser(user: User, conversationId: String) {
        if (this.conversationId == conversationId) {
            users = addUserToList(user, users)
        } else {
            val groupCallState = pendingGroupCalls.find {
                it.conversationId == conversationId
            } ?: return
            groupCallState.users = addUserToList(user, groupCallState.users)
        }
    }

    fun removeUser(user: User, conversationId: String) {
        if (this.conversationId == conversationId) {
            if (users.isNullOrEmpty()) return

            users?.remove(user.userId)
        } else {
            val groupCallState = pendingGroupCalls.find {
                it.conversationId == conversationId
            } ?: return
            groupCallState.users?.remove(user.userId)
        }
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

    private fun addUserToList(user: User, users: ArrayList<String>? = null): ArrayList<String> {
        val userList = users ?: arrayListOf()
        userList.let { us ->
            val existsUser = us.find { u -> u == user.userId }
            if (existsUser == null) {
                us.add(user.userId)
            }
        }
        return userList
    }
}
