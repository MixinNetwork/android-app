package one.mixin.android.vo

import android.content.Context
import androidx.lifecycle.LiveData
import one.mixin.android.webrtc.CallService

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
    var users: ArrayList<User>? = null
    var connectedTime: Long? = null
    var isOffer: Boolean = true

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

    fun addUser(user: User) {
        if (users == null) {
            users = arrayListOf()
        }
        users?.let { us ->
            val existsUser = us.find { u -> u.userId == user.userId }
            if (existsUser != null) return

            us.add(user)
        }
    }

    fun removeUser(user: User) {
        if (users.isNullOrEmpty()) return

        users?.remove(user) ?: false
    }

    fun isIdle() = state == CallService.CallState.STATE_IDLE
    fun isConnected() = state == CallService.CallState.STATE_CONNECTED

    fun handleHangup(ctx: Context) {
        when (state) {
            CallService.CallState.STATE_DIALING ->
                if (isGroupCall()) {
                    CallService.krakenCancel(ctx)
                } else {
                    CallService.cancel(ctx)
                }
            CallService.CallState.STATE_RINGING ->
                if (isGroupCall()) {
                    CallService.krakenDecline(ctx)
                } else {
                    CallService.decline(ctx)
                }
            CallService.CallState.STATE_ANSWERING -> {
                if (isOffer) {
                    CallService.cancel(ctx)
                } else {
                    CallService.decline(ctx)
                }
            }
            CallService.CallState.STATE_CONNECTED ->
                if (isGroupCall()) {
                    CallService.krakenEnd(ctx)
                } else {
                    CallService.localEnd(ctx)
                }
            else -> CallService.cancel(ctx)
        }
    }
}
