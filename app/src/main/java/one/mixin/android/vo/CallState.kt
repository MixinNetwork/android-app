package one.mixin.android.vo

import androidx.lifecycle.LiveData
import one.mixin.android.webrtc.CallService

class CallState : LiveData<CallState.CallInfo>() {
    var callInfo: CallInfo = CallInfo()

    fun setCallState(callState: CallService.CallState) {
        if (callInfo.callState == callState) return

        callInfo = CallInfo(callState, callInfo.dialingStatus)
        postValue(callInfo)
    }

    fun setDialingStatus(dialingStatus: MessageStatus) {
        if (callInfo.dialingStatus == dialingStatus) return

        callInfo = CallInfo(callInfo.callState, dialingStatus)
        postValue(callInfo)
    }

    fun setMessageId(messageId: String) {
        if (callInfo.messageId == messageId) return

        callInfo = CallInfo(callInfo.callState, callInfo.dialingStatus, messageId)
        postValue(callInfo)
    }

    class CallInfo(
        val callState: CallService.CallState = CallService.CallState.STATE_IDLE,
        val dialingStatus: MessageStatus = MessageStatus.SENDING,
        val messageId: String? = null
    )
}