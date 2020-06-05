package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.view_link_state.*
import one.mixin.android.R
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.di.Injectable
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.LinkState
import one.mixin.android.webrtc.CallService
import org.jetbrains.anko.runOnUiThread
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

open class LinkFragment : BaseFragment(), Injectable, Observer<Int> {

    @Inject
    lateinit var linkState: LinkState
    @Inject
    lateinit var callState: CallStateLiveData
    @Inject
    lateinit var floodMessageDao: FloodMessageDao

    private lateinit var floodMessageCount: LiveData<Int>

    private var barShown = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        floodMessageCount = floodMessageDao.getFloodMessageCount()
        linkState.observe(
            viewLifecycleOwner,
            Observer { state ->
                check(state)
            }
        )
        callState.observe(
            viewLifecycleOwner,
            Observer {
                check(linkState.state)
            }
        )
    }

    override fun onStop() {
        stopTimber()
        super.onStop()
    }

    override fun onResume() {
        if (callState.connectedTime != null) {
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
            floodMessageCount.observe(viewLifecycleOwner, this)
            hiddenBar()
        } else {
            floodMessageCount.removeObserver(this)
            setConnecting()
            showBar()
        }
    }

    override fun onChanged(t: Int?) {
        if (callState.callInfo.callState != CallService.CallState.STATE_IDLE) return

        t.notNullWithElse(
            {
                if (it > 500) {
                    setSyncing()
                    showBar()
                } else {
                    hiddenBar()
                }
            },
            {
                hiddenBar()
            }
        )
    }

    private fun showBar() {
        if (!barShown) {
            state_layout.animateHeight(0, requireContext().dpToPx(26f))
            barShown = true
        }
    }

    private fun hiddenBar() {
        if (barShown) {
            state_layout.animateHeight(requireContext().dpToPx(26f), 0)
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
        if (callState.connectedTime != null) {
            time_tv.visibility = VISIBLE
            startTimer()
        } else {
            time_tv.visibility = GONE
        }
        state_layout.setBackgroundResource(R.color.stateGreen)
        progressBar.visibility = GONE
        state_tv.setText(R.string.state_calling)
        state_layout.setOnClickListener {
            CallActivity.show(requireContext(), callState.user)
        }
    }

    private var timer: Timer? = null

    private fun startTimer() {
        timer = Timer(true)
        val timerTask = object : TimerTask() {
            override fun run() {
                if (isAdded) {
                    requireContext().runOnUiThread {
                        if (callState.connectedTime != null) {
                            val duration = System.currentTimeMillis() - callState.connectedTime!!
                            time_tv?.text = duration.formatMillis()
                        }
                    }
                }
            }
        }
        timer?.schedule(timerTask, 0, 1000)
    }

    private fun stopTimber() {
        timer?.cancel()
        timer?.purge()
        timer = null
    }
}
