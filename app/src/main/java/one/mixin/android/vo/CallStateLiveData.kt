package one.mixin.android.vo

import android.content.Context
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import androidx.lifecycle.LiveData
import one.mixin.android.util.Session
import one.mixin.android.webrtc.CallService
import one.mixin.android.webrtc.cancelCall
import one.mixin.android.webrtc.declineCall
import one.mixin.android.webrtc.krakenCancel
import one.mixin.android.webrtc.krakenCancelSilently
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

enum class CallType {
    None, Voice, Group
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

    var callType = CallType.None

    private val pendingGroupCalls = mutableSetOf<GroupCallState>()

    fun reset() {
        conversationId?.let {
            clearInitialGuests(it)
            removeUser(it, Session.getAccountId()!!)
        }
        conversationId = null
        trackId = null
        user = null
        connectedTime = null
        isOffer = true
        audioEnable = true
        speakerEnable = false
        callType = CallType.None
        state = CallService.CallState.STATE_IDLE
    }

    fun isGroupCall() = callType == CallType.Group
    fun isVoiceCall() = callType == CallType.Voice

    fun isBusy(ctx: Context): Boolean {
        val tm = ctx.getSystemService<TelephonyManager>()
        return isNotIdle() || tm?.callState != TelephonyManager.CALL_STATE_IDLE
    }

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
        return groupCallState.users
    }

    fun getUsersCountByConversationId(conversationId: String): Int =
        getUsersByConversationId(conversationId)?.size ?: 0

    fun setInitialGuests(conversationId: String, guests: ArrayList<String>) {
        if (guests.isNullOrEmpty()) return

        val groupCallState = addPendingGroupCall(conversationId)
        groupCallState.initialGuests = guests
    }

    fun addInitialGuests(conversationId: String, guests: ArrayList<String>) {
        if (guests.isNullOrEmpty()) return

        val groupCallState = addPendingGroupCall(conversationId)

        val current = groupCallState.initialGuests
        if (current.isNullOrEmpty()) {
            groupCallState.initialGuests = guests
            postValue(state)
            return
        }
        groupCallState.initialGuests = arrayListOf<String>().apply { addAll(current.union(guests)) }
        postValue(state)
    }

    fun removeInitialGuest(conversationId: String, userId: String) {
        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return
        groupCallState.initialGuests?.remove(userId)
        postValue(state)
    }

    fun clearInitialGuests(conversationId: String) {
        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return
        groupCallState.initialGuests = null
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

    fun getUsersWithGuests(conversationId: String): ArrayList<String>? {
        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return null
        val us = groupCallState.users
        val initialGuests = groupCallState.initialGuests

        if (initialGuests.isNullOrEmpty()) return us
        if (us.isNullOrEmpty()) return initialGuests

        return arrayListOf<String>().apply { addAll(initialGuests.union(us)) }
    }

    fun setUsersByConversationId(conversationId: String, newUsers: ArrayList<String>?) {
        if (newUsers.isNullOrEmpty()) return

        val groupCallState = addPendingGroupCall(conversationId)
        groupCallState.users = newUsers

        postValue(state)
    }

    fun addUser(conversationId: String, userId: String) {
        val groupCallState = addPendingGroupCall(conversationId)
        groupCallState.users = addUserToList(userId, groupCallState.users)

        postValue(state)
    }

    fun removeUser(conversationId: String, userId: String) {
        val groupCallState = pendingGroupCalls.find {
            it.conversationId == conversationId
        } ?: return
        groupCallState.users?.remove(userId)

        postValue(state)
    }

    fun isIdle() = state == CallService.CallState.STATE_IDLE
    fun isNotIdle() = state != CallService.CallState.STATE_IDLE
    fun isAnswering() = state == CallService.CallState.STATE_ANSWERING
    fun isConnected() = state == CallService.CallState.STATE_CONNECTED
    fun isRinging() = state == CallService.CallState.STATE_RINGING
    fun isBeforeAnswering() = state < CallService.CallState.STATE_ANSWERING

    fun isPendingGroupCall(conversationId: String) =
        if (this.conversationId == conversationId) {
            isIdle()
        } else {
            pendingGroupCalls.any { it.conversationId == conversationId }
        }

    fun handleHangup(ctx: Context, join: Boolean = false) {
        when (state) {
            CallService.CallState.STATE_DIALING ->
                if (isGroupCall()) {
                    krakenCancel(ctx)
                } else if (isVoiceCall()) {
                    cancelCall(ctx)
                }
            CallService.CallState.STATE_RINGING ->
                if (isGroupCall()) {
                    if (join) {
                        krakenCancelSilently(ctx)
                    } else {
                        krakenDecline(ctx)
                    }
                } else if (isVoiceCall()) {
                    declineCall(ctx)
                }
            CallService.CallState.STATE_ANSWERING -> {
                if (isGroupCall()) {
                    krakenCancel(ctx)
                } else if (isVoiceCall()) {
                    if (isOffer) {
                        cancelCall(ctx)
                    } else {
                        declineCall(ctx)
                    }
                }
            }
            CallService.CallState.STATE_CONNECTED ->
                if (isGroupCall()) {
                    krakenEnd(ctx)
                } else if (isVoiceCall()) {
                    localEnd(ctx)
                }
            else ->
                if (isGroupCall()) {
                    krakenCancel(ctx)
                } else if (isVoiceCall()) {
                    cancelCall(ctx)
                }
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
