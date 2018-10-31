package one.mixin.android.vo

import androidx.lifecycle.LiveData
import one.mixin.android.webrtc.CallService

class CallState : LiveData<CallState.CallInfo>() {
    var callInfo: CallInfo = CallInfo()

    fun setCallState(callState: CallService.CallState) {
        if (callInfo.callState == callState) return

        callInfo = CallInfo(callState, callInfo.dialingStatus, callInfo.messageId, callInfo.user, callInfo.connectedTime)
        postValue(callInfo)
    }

    fun setDialingStatus(dialingStatus: MessageStatus) {
        if (callInfo.dialingStatus == dialingStatus) return

        callInfo = CallInfo(callInfo.callState, dialingStatus, callInfo.messageId, callInfo.user, callInfo.connectedTime)
        postValue(callInfo)
    }

    fun setMessageId(messageId: String) {
        if (callInfo.messageId == messageId) return

        callInfo = CallInfo(callInfo.callState, callInfo.dialingStatus, messageId, callInfo.user, callInfo.connectedTime)
        postValue(callInfo)
    }

    fun setUser(user: User?) {
        if (callInfo.user == user) return

        callInfo = CallInfo(callInfo.callState, callInfo.dialingStatus, callInfo.messageId, user, callInfo.connectedTime)
    }

    fun setConnectedTime(connectedTime: Long?) {
        if (callInfo.connectedTime == connectedTime) return

        callInfo = CallInfo(callInfo.callState, callInfo.dialingStatus, callInfo.messageId, callInfo.user, connectedTime)
    }

    fun reset() {
        callInfo = CallInfo()
        postValue(callInfo)
    }

    fun isIdle() = callInfo.callState == CallService.CallState.STATE_IDLE

    class CallInfo(
        val callState: CallService.CallState = CallService.CallState.STATE_IDLE,
        val dialingStatus: MessageStatus = MessageStatus.SENDING,
        val messageId: String? = null,
        val user: User? = null,
        val connectedTime: Long? = null
    )
}