package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.view_link_state.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.di.Injectable
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.notNullElse
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.vo.CallState
import one.mixin.android.vo.LinkState
import one.mixin.android.webrtc.CallService
import javax.inject.Inject

open class LinkFragment : BaseFragment(), Injectable, Observer<Int> {

    @Inject
    lateinit var linkState: LinkState
    @Inject
    lateinit var callState: CallState
    @Inject
    lateinit var floodMessageDao: FloodMessageDao

    private lateinit var floodMessageCount: LiveData<Int>

    private var barShown = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        floodMessageCount = floodMessageDao.getFloodMessageCount()
        linkState.observe(this, Observer { state ->
            check(state)
        })
        callState.observe(this, Observer { _ ->
            check(linkState.state)
        })
    }

    override fun onStop() {
        stopTimber()
        super.onStop()
    }

    override fun onResume() {
        if (callState.callInfo.connectedTime != null) {
            time_tv.visibility = VISIBLE
            startTimer()
        }
        super.onResume()
    }

    @Synchronized
    private fun check(state: Int?) {
        if (callState.callInfo.callState != CallService.CallState.STATE_IDLE) {
            setCalling()
            showBar()
            return
        }

        if (LinkState.isOnline(state)) {
            state_layout.animateHeight(context!!.dpToPx(26f), 0)
            floodMessageCount.observe(this, this)
            barShown = false
        } else {
            floodMessageCount.removeObserver(this)
            barShown = false
            setConnecting()
            state_layout.animateHeight(0, context!!.dpToPx(26f))
        }
    }

    override fun onChanged(t: Int?) {
        if (callState.callInfo.callState != CallService.CallState.STATE_IDLE) return

        notNullElse(t, {
            if (it > 500) {
                setSyncing()
                showBar()
            } else {
                hiddenBar()
            }
        }, {
            hiddenBar()
        })
    }

    private fun showBar() {
        if (!barShown) {
            state_layout.animateHeight(0, context!!.dpToPx(26f))
            barShown = true
        }
    }

    private fun hiddenBar() {
        if (barShown) {
            state_layout.animateHeight(context!!.dpToPx(26f), 0)
            barShown = false
        }
    }

    private fun setConnecting() {
        progressBar.visibility = VISIBLE
        time_tv.visibility = GONE
        stopTimber()
        state_layout.setBackgroundResource(R.color.colorBlue)
        state_tv.setText(R.string.state_connecting)
    }

    private fun setSyncing() {
        progressBar.visibility = VISIBLE
        time_tv.visibility = GONE
        stopTimber()
        state_layout.setBackgroundResource(R.color.stateGreen)
        state_tv.setText(R.string.state_syncing)
    }

    private fun setCalling() {
        if (callState.callInfo.connectedTime != null) {
            time_tv.visibility = VISIBLE
            startTimer()
        }
        state_layout.setBackgroundResource(R.color.stateGreen)
        progressBar.visibility = GONE
        state_tv.setText(R.string.state_calling)
        state_layout.setOnClickListener {
            CallActivity.show(requireContext(), callState.callInfo.user)
        }
    }

    private var timer: Job? = null
    private fun startTimer() {
        if (timer == null || timer?.isCancelled == true) {
            timer = GlobalScope.launch {
                while (true) {
                    withContext(Dispatchers.Main) {
                        if (callState.callInfo.connectedTime != null) {
                            val duration = System.currentTimeMillis() - callState.callInfo.connectedTime!!
                            time_tv?.text = duration.formatMillis()
                        }
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun stopTimber() {
        if (timer?.isCancelled == false) {
            timer?.cancel()
            timer = null
        }
    }
}