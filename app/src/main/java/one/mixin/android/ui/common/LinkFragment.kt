package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import kotlinx.android.synthetic.main.view_link_state.*
import one.mixin.android.R
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.di.Injectable
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.util.StateManager
import one.mixin.android.vo.CallState
import one.mixin.android.vo.LinkState
import org.jetbrains.anko.runOnUiThread
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

open class LinkFragment : BaseFragment(), Injectable {

    @Inject
    lateinit var linkState: LinkState
    @Inject
    lateinit var callState: CallState
    @Inject
    lateinit var floodMessageDao: FloodMessageDao

    private var barShown = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        StateManager(linkState, callState, floodMessageDao, this,
            object : StateManager.Callback {
                override fun onCalling() {
                    setCalling()
                    showBar()
                }

                override fun onConnecting() {
                    setConnecting()
                    showBar()
                }

                override fun onSyncing() {
                    setSyncing()
                    showBar()
                }

                override fun onNormal() {
                    hideBar()
                }
            })
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

    private fun showBar() {
        if (!barShown) {
            state_layout.animateHeight(0, context!!.dpToPx(26f))
            barShown = true
        }
    }

    private fun hideBar() {
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