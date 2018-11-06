package one.mixin.android.vo

import android.content.Context
import androidx.lifecycle.LiveData
import one.mixin.android.webrtc.CallService

class CallState : LiveData<CallState.CallInfo>() {
    var callInfo: CallInfo = CallInfo()
    var user: User? = null
    var connectedTime: Long? = null
    var isInitiator: Boolean = true

    fun setCallState(callState: CallService.CallState) {
        if (callInfo.callState == callState) return

        callInfo = CallInfo(callState, callInfo.messageId)
        postValue(callInfo)
    }

    fun setMessageId(messageId: String) {
        if (callInfo.messageId == messageId) return

        callInfo = CallInfo(callInfo.callState, messageId)
        postValue(callInfo)
    }

    fun reset() {
        callInfo = CallInfo()
        user = null
        connectedTime = null
        isInitiator = true
        postValue(callInfo)
    }

    fun isIdle() = callInfo.callState == CallService.CallState.STATE_IDLE

    fun handleHangup(ctx: Context) {
        when (callInfo.callState) {
            CallService.CallState.STATE_DIALING -> CallService.startService(ctx, CallService.ACTION_CALL_CANCEL)
            CallService.CallState.STATE_RINGING -> CallService.startService(ctx, CallService.ACTION_CALL_DECLINE)
            CallService.CallState.STATE_ANSWERING -> {
                if (isInitiator) {
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
        val messageId: String? = null
    )
}