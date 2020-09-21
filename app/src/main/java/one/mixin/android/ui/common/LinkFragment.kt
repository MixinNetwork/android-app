package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.view_link_state.*
import one.mixin.android.R
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.LinkState
import javax.inject.Inject

open class LinkFragment : BaseFragment(), Observer<Int> {

    @Inject
    lateinit var linkState: LinkState
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
    }

    @Synchronized
    private fun check(state: Int?) {
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
        state_layout.setBackgroundResource(R.color.colorBlue)
        state_tv.setText(R.string.state_connecting)
    }

    private fun setSyncing() {
        progressBar.visibility = VISIBLE
        state_layout.setBackgroundResource(R.color.stateGreen)
        state_tv.setText(R.string.state_syncing)
    }
}
