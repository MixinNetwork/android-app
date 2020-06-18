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
    var inviter: String? = null
    var users: ArrayList<String>? = null
    var initialGuests: ArrayList<String>? = null
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

    var audioEnable = true
    var speakerEnable = false

    private val pendingGroupCalls = mutableSetOf<GroupCallState>()

    fun reset() {
        conversationId = null
        trackId = null
        user = null
        connectedTime = null
        isOffer = true
        audioEnable = true
        speakerEnable = false
        state = CallService.CallState.STATE_IDLE
    }

    fun isGroupCall() = user == null

    fun addPendingGroupCall(conversationId: String): GroupCallState {
        val exists = pendingGroupCalls.find {
            it.conversationId == conversationId
        }
        if (exists != null) return exists

        val groupCallState = GroupCallState(conversationId)
        pendingGroupCalls.add(groupCallState)
        return groupCallState
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

    fun setInviter(conversationId: String, userId: String) {
        val groupCallState = addPendingGroupCall(conversationId)
        groupCallState.inviter = userId
    }

    fun getInviter(conversationId: String): String? =
        pendingGroupCalls.find {
            it.conversationId == conversationId
        }?.inviter

    fun getUsersByConversationId(conversationId: String): ArrayList<String>? {
        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return null
        val us = groupCallState.users
        val initialGuests = groupCallState.initialGuests

        if (initialGuests.isNullOrEmpty()) return us
        if (us.isNullOrEmpty()) return initialGuests

        return arrayListOf<String>().apply { addAll(initialGuests.union(us)) }
    }

    fun getUsersCountByConversationId(conversationId: String): Int =
        getUsersByConversationId(conversationId)?.size ?: 0

    fun setInitialGuests(conversationId: String, guests: ArrayList<String>) {
        if (guests.isNullOrEmpty()) return

        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return
        groupCallState.initialGuests = guests
    }

    fun addInitialGuests(conversationId: String, guests: ArrayList<String>) {
        if (guests.isNullOrEmpty()) return

        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return

        val current = groupCallState.initialGuests
        if (current.isNullOrEmpty()) {
            groupCallState.initialGuests = guests
            return
        }
        groupCallState.initialGuests = arrayListOf<String>().apply { addAll(current.union(guests)) }
    }

    fun removeInitialGuest(conversationId: String, userId: String) {
        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return
        groupCallState.initialGuests?.remove(userId)
    }

    fun getGuestsNotInUsers(conversationId: String): ArrayList<String>? {
        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return null
        val us = groupCallState.users
        val initialGuests = groupCallState.initialGuests

        if (initialGuests.isNullOrEmpty()) return null
        if (us.isNullOrEmpty()) return initialGuests

        val resultSet = initialGuests.subtract(us)
        if (resultSet.isNullOrEmpty()) return null

        return arrayListOf<String>().apply { addAll(resultSet) }
    }

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
                    val cid = conversationId
                    requireNotNull(cid)
                    krakenDecline(ctx, cid)
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
