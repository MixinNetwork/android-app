package one.mixin.android.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.extension.notNullElse
import one.mixin.android.vo.CallState
import one.mixin.android.vo.LinkState
import one.mixin.android.webrtc.CallService

class StateManager(
    private val linkState: LinkState,
    private val callState: CallState,
    floodMessageDao: FloodMessageDao,
    private val owner: LifecycleOwner,
    private val callback: Callback
): Observer<Int> {
    private val floodMessageCount: LiveData<Int> = floodMessageDao.getFloodMessageCount()

    init {
        linkState.observe(owner, Observer { state ->
            check(state)
        })
        callState.observe(owner, Observer {
            check(linkState.state)
        })
    }

    private fun check(state: Int?) {
        if (callState.callInfo.callState != CallService.CallState.STATE_IDLE) {
            callback.onCalling()
            return
        }

        if (LinkState.isOnline(state)) {
            floodMessageCount.observe(owner, this)
            callback.onNormal()
        } else {
            floodMessageCount.removeObserver(this)
            callback.onConnecting()
        }
    }

    override fun onChanged(t: Int?) {
        if (callState.callInfo.callState != CallService.CallState.STATE_IDLE) return

        notNullElse(t, {
            if (it > 500) {
                callback.onSyncing()
            } else {
                callback.onNormal()
            }
        }, {
            callback.onNormal()
        })
    }

    interface Callback {
        fun onCalling()
        fun onConnecting()
        fun onSyncing()
        fun onNormal()
    }
}