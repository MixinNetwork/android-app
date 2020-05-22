package one.mixin.android.vo

import android.content.Context
import androidx.lifecycle.LiveData
import one.mixin.android.webrtc.CallService

class CallStateLiveData : LiveData<CallStateLiveData.CallInfo>() {
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
            CallService.CallState.STATE_DIALING -> CallService.cancel(ctx)
            CallService.CallState.STATE_RINGING -> CallService.decline(ctx)
            CallService.CallState.STATE_ANSWERING -> {
                if (isInitiator) {
                    CallService.cancel(ctx)
                } else {
                    CallService.decline(ctx)
                }
            }
            CallService.CallState.STATE_CONNECTED -> CallService.localEnd(ctx)
            else -> CallService.cancel(ctx)
        }
    }

    class CallInfo(
        val callState: CallService.CallState = CallService.CallState.STATE_IDLE,
        val messageId: String? = null
    )
}
