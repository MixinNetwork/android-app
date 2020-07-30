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

data class GroupCallUser(
    val id: String,
    var type: Type
) {
    enum class Type {
        Joined, Pending
    }
}

data class GroupCallState(
    var conversationId: String
) {
    var inviter: String? = null
    var users: List<GroupCallUser>? = null

    var lock = Any()

    fun userIds(): List<String>? {
        if (users.isNullOrEmpty()) return null

        val ids = mutableListOf<String>()
        users?.mapTo(ids) { it.id }
        return ids
    }

    fun pendingUserIds(): List<String>? {
        if (users.isNullOrEmpty()) return null

        val ids = mutableListOf<String>()
        users?.filter { it.type == GroupCallUser.Type.Pending }
            ?.mapTo(ids) { it.id }
        return ids
    }

    fun addJoinedUser(inUser: String) {
        addUserToList(inUser, GroupCallUser.Type.Joined)
    }

    fun removeUser(inUser: String) {
        if (users.isNullOrEmpty()) return

        synchronized(lock) {
            val us = mutableListOf<GroupCallUser>().apply {
                users?.let { addAll(it) }
            }
            val each = us.iterator()
            while (each.hasNext()) {
                if (inUser == each.next().id) {
                    each.remove()
                }
            }
            users = us
        }
    }

    fun setJoinedUsers(joinedUsers: List<String>) {
        synchronized(lock) {
            val us = mutableListOf<GroupCallUser>().apply {
                users?.let { addAll(it) }
            }
            if (us.isEmpty()) {
                joinedUsers.forEach {
                    us.add(GroupCallUser(it, GroupCallUser.Type.Joined))
                }
            } else {
                val each = us.iterator()
                while (each.hasNext()) {
                    val next = each.next()
                    val exists = joinedUsers.find { it == next.id }
                    if (exists == null) {
                        each.remove()
                    } else {
                        if (next.type == GroupCallUser.Type.Pending) {
                            next.type = GroupCallUser.Type.Joined
                        }
                    }
                }
            }
            users = us
        }
    }

    fun addPendingUsers(pendingUsers: List<String>) {
        addUsersToList(pendingUsers, GroupCallUser.Type.Pending)
    }

    fun clearPendingUsers() {
        val us = users
        if (us.isNullOrEmpty()) return

        synchronized(lock) {
            val newUsers = mutableListOf<GroupCallUser>()
            us.forEach { u ->
                if (u.type == GroupCallUser.Type.Joined) {
                    newUsers.add(u)
                }
            }
            users = newUsers
        }
    }

    private fun addUserToList(userId: String, type: GroupCallUser.Type) {
        synchronized(lock) {
            val us = mutableListOf<GroupCallUser>().apply {
                users?.let { addAll(it) }
            }
            changeOrAnd(us, userId, type)
            users = us
        }
    }

    private fun addUsersToList(userIds: List<String>, type: GroupCallUser.Type) {
        synchronized(lock) {
            val us = mutableListOf<GroupCallUser>().apply {
                users?.let { addAll(it) }
            }
            userIds.forEach { id ->
                changeOrAnd(us, id, type)
            }
            users = us
        }
    }

    private fun changeOrAnd(us: MutableList<GroupCallUser>, id: String, type: GroupCallUser.Type) {
        synchronized(lock) {
            val existsUser = us.find { u -> u.id == id }
            if (existsUser == null) {
                us.add(0, GroupCallUser(id, type))
            } else {
                if (type == GroupCallUser.Type.Joined) {
                    existsUser.type = type
                }
            }
        }
    }
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

    var disconnected = false
        set(value) {
            if (field == value) return

            field = value
            postValue(state)
        }
    var reconnecting = false

    var audioEnable = true
    var speakerEnable = false
    var customAudioDeviceAvailable = false
        set(value) {
            if (field == value) return

            field = value
            postValue(state)
        }

    var callType = CallType.None

    private val groupCallStates = mutableSetOf<GroupCallState>()

    private val lock = Any()

    fun reset() {
        conversationId?.let {
            removeUser(it, Session.getAccountId()!!)
            clearPendingUsers(it)
        }
        conversationId = null
        trackId = null
        user = null
        connectedTime = null
        isOffer = true
        disconnected = false
        reconnecting = false
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

    fun getGroupCallStateOrNull(conversationId: String): GroupCallState? {
        return groupCallStates.find {
            it.conversationId == conversationId
        }
    }

    fun addGroupCallState(conversationId: String): GroupCallState {
        val exists = groupCallStates.find {
            it.conversationId == conversationId
        }
        if (exists != null) return exists

        val groupCallState = GroupCallState(conversationId)
        groupCallStates.add(groupCallState)
        return groupCallState
    }

    fun removeGroupCallState(conversationId: String): Boolean {
        val each = groupCallStates.iterator()
        var removed = false
        while (each.hasNext()) {
            if (conversationId == each.next().conversationId) {
                each.remove()
                removed = true
            }
        }
        postValue(state)
        return removed
    }

    fun setInviter(conversationId: String, userId: String) {
        val groupCallState = addGroupCallState(conversationId)
        groupCallState.inviter = userId
    }

    fun getInviter(conversationId: String): String? =
        groupCallStates.find {
            it.conversationId == conversationId
        }?.inviter

    fun getUsers(conversationId: String): List<String>? {
        val groupCallState = groupCallStates.find {
            it.conversationId == conversationId
        } ?: return null
        return groupCallState.userIds()
    }

    fun getUsersCount(conversationId: String): Int =
        getUsers(conversationId)?.size ?: 0

    fun setPendingUsers(conversationId: String, guests: List<String>) {
        if (guests.isNullOrEmpty()) return

        val groupCallState = addGroupCallState(conversationId)
        groupCallState.addPendingUsers(guests)
    }

    fun addPendingUsers(conversationId: String, guests: List<String>) {
        if (guests.isNullOrEmpty()) return

        val groupCallState = addGroupCallState(conversationId)

        val current = groupCallState.users
        if (current.isNullOrEmpty()) {
            groupCallState.addPendingUsers(guests)
            postValue(state)
            return
        }
        groupCallState.addPendingUsers(guests)
        postValue(state)
    }

    fun clearUsersKeepSelf(conversationId: String) {
        synchronized(lock) {
            val groupCallState = groupCallStates.find {
                it.conversationId == conversationId
            } ?: return
            groupCallState.users = listOf(GroupCallUser(Session.getAccountId()!!, GroupCallUser.Type.Joined))
        }

        postValue(state)
    }

    fun clearPendingUsers(conversationId: String) {
        val groupCallState = groupCallStates.find {
            it.conversationId == conversationId
        } ?: return
        groupCallState.clearPendingUsers()
    }

    fun getPendingUsers(conversationId: String): List<String>? {
        val groupCallState = groupCallStates.find {
            it.conversationId == conversationId
        } ?: return null
        return groupCallState.pendingUserIds()
    }

    fun setUsersByConversationId(conversationId: String, newUsers: List<String>?) {
        if (newUsers.isNullOrEmpty()) return

        synchronized(lock) {
            val groupCallState = addGroupCallState(conversationId)

            val self = Session.getAccountId()!!
            if (!isBeforeAnswering() && !newUsers.contains(self)) {
                groupCallState.setJoinedUsers(
                    mutableListOf<String>().apply {
                        add(self)
                        addAll(newUsers)
                    }
                )
            } else {
                groupCallState.setJoinedUsers(newUsers)
            }
        }

        postValue(state)
    }

    fun addUser(conversationId: String, userId: String) {
        val groupCallState = addGroupCallState(conversationId)
        groupCallState.addJoinedUser(userId)

        postValue(state)
    }

    fun removeUser(conversationId: String, userId: String) {
        val groupCallState = groupCallStates.find {
            it.conversationId == conversationId
        } ?: return
        groupCallState.removeUser(userId)

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
            groupCallStates.any { it.conversationId == conversationId }
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
}
