package one.mixin.android.vo

import android.content.Context
import androidx.lifecycle.LiveData
import one.mixin.android.webrtc.CallService

class CallState : LiveData<CallState.CallInfo>() {
    var callInfo: CallInfo = CallInfo()

    fun setCallState(callState: CallService.CallState) {
        if (callInfo.callState == callState) return

        callInfo = CallInfo(callState, callInfo.dialingStatus, callInfo.messageId, callInfo.user, callInfo.connectedTime, callInfo.isInitiator)
        postValue(callInfo)
    }

    fun setDialingStatus(dialingStatus: MessageStatus) {
        if (callInfo.dialingStatus == dialingStatus) return

        callInfo = CallInfo(callInfo.callState, dialingStatus, callInfo.messageId, callInfo.user, callInfo.connectedTime, callInfo.isInitiator)
        postValue(callInfo)
    }

    fun setMessageId(messageId: String) {
        if (callInfo.messageId == messageId) return

        callInfo = CallInfo(callInfo.callState, callInfo.dialingStatus, messageId, callInfo.user, callInfo.connectedTime, callInfo.isInitiator)
        postValue(callInfo)
    }

    fun setUser(user: User?) {
        if (callInfo.user == user) return

        callInfo = CallInfo(callInfo.callState, callInfo.dialingStatus, callInfo.messageId, user, callInfo.connectedTime, callInfo.isInitiator)
    }

    fun setConnectedTime(connectedTime: Long?) {
        if (callInfo.connectedTime == connectedTime) return

        callInfo = CallInfo(callInfo.callState, callInfo.dialingStatus, callInfo.messageId, callInfo.user, connectedTime, callInfo.isInitiator)
    }

    fun setIsInitiator(isInitiator: Boolean) {
        if (callInfo.isInitiator == isInitiator) return

        callInfo = CallInfo(callInfo.callState, callInfo.dialingStatus, callInfo.messageId, callInfo.user, callInfo.connectedTime, isInitiator)
    }

    fun reset() {
        callInfo = CallInfo()
        postValue(callInfo)
    }

    fun isIdle() = callInfo.callState == CallService.CallState.STATE_IDLE
    
    fun handleHangup(ctx: Context) {
        when (callInfo.callState) {
            CallService.CallState.STATE_DIALING -> CallService.startService(ctx, CallService.ACTION_CALL_CANCEL)
            CallService.CallState.STATE_RINGING -> CallService.startService(ctx, CallService.ACTION_CALL_DECLINE)
            CallService.CallState.STATE_ANSWERING -> {
                if (callInfo.isInitiator) {
                    CallService.startService(ctx, CallService.ACTION_CALL_CANCEL)
                } else {
                    CallService.startService(ctx, CallService.ACTION_CALL_DECLINE)
                }
            }
            CallService.CallState.STATE_CONNECTED -> CallService.startService(ctx, CallService.ACTION_CALL_LOCAL_END)
            else -> CallService.startService(ctx, CallService.ACTION_CALL_CANCEL)
        }
    }

    class CallInfo(
        val callState: CallService.CallState = CallService.CallState.STATE_IDLE,
        val dialingStatus: MessageStatus = MessageStatus.SENDING,
        val messageId: String? = null,
        val user: User? = null,
        val connectedTime: Long? = null,
        val isInitiator: Boolean = true
    )
}