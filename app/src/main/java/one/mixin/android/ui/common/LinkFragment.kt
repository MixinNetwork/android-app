package one.mixin.android.ui.common

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.os.Bundle
import kotlinx.android.synthetic.main.view_link_state.*
import one.mixin.android.R
import one.mixin.android.db.MessageDao
import one.mixin.android.di.Injectable
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.notNullElse
import one.mixin.android.vo.LinkState
import javax.inject.Inject

open class LinkFragment : BaseFragment(), Injectable, Observer<Int> {

    @Inject
    lateinit var linkState: LinkState
    @Inject
    lateinit var messageDao: MessageDao

    private lateinit var floodMessageCount: LiveData<Int>

    private var barShown = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        floodMessageCount = messageDao.getFloodMessageCount()
        linkState.observe(this, Observer { state ->
            check(state)
        })
    }

    private fun check(state: Int?) {
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
        state_layout.setBackgroundResource(R.color.colorBlue)
        state_tv.setText(R.string.state_connecting)
    }

    private fun setSyncing() {
        state_layout.setBackgroundResource(R.color.stateGreen)
        state_tv.setText(R.string.state_syncing)
    }
}