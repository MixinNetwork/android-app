package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import one.mixin.android.R
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.db.pending.PendingMessageDao
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.networkConnected
import one.mixin.android.ui.setting.diagnosis.DiagnosisActivity
import one.mixin.android.vo.LinkState
import javax.inject.Inject

abstract class LinkFragment : BaseFragment(), Observer<Int> {

    @Inject
    lateinit var linkState: LinkState

    @Inject
    lateinit var floodMessageDao: FloodMessageDao

    @Inject
    lateinit var pendingMessageDao: PendingMessageDao

    private lateinit var processedCount: LiveData<Int>

    private var barShown = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        processedCount = floodMessageDao.getFloodMessageCount()
            .combineWith(pendingMessageDao.getPendingMessageCount()) { a, b ->
                (a ?: 0) + (b ?: 0)
            }
        linkState.observe(viewLifecycleOwner) { state ->
            check(state)
        }
    }

    private fun <T, K, R> LiveData<T>.combineWith(
        liveData: LiveData<K>,
        block: (T?, K?) -> R,
    ): LiveData<R> {
        val result = MediatorLiveData<R>()
        result.addSource(this) {
            result.value = block(this.value, liveData.value)
        }
        result.addSource(liveData) {
            result.value = block(this.value, liveData.value)
        }
        return result
    }

    @Synchronized
    private fun check(state: Int?) {
        if (LinkState.isOnline(state)) {
            processedCount.observe(viewLifecycleOwner, this)
            hiddenBar()
        } else {
            processedCount.removeObserver(this)
            setConnecting()
            showBar()
        }
    }

    override fun onChanged(value: Int) {
        if (value > 500) {
            setSyncing()
            showBar()
        } else {
            hiddenBar()
        }
    }

    abstract fun getContentView(): View
    private val _contentView get() = getContentView()
    private val stateLayout: View by lazy {
        _contentView.findViewById(R.id.state_layout)
    }
    private val progressBar: View by lazy {
        _contentView.findViewById(R.id.progressBar)
    }
    private val stateTv: TextView by lazy {
        _contentView.findViewById(R.id.state_tv)
    }

    private fun showBar() {
        if (!barShown) {
            stateLayout.animateHeight(0, requireContext().dpToPx(26f))
            barShown = true
        }
    }

    private fun hiddenBar() {
        if (barShown) {
            stateLayout.animateHeight(requireContext().dpToPx(26f), 0)
            barShown = false
        }
    }

    private fun setConnecting() {
        stateLayout.setBackgroundResource(R.color.colorBlue)
        val networkAvailable = requireContext().networkConnected()
        if (networkAvailable) {
            progressBar.isVisible = true
            stateTv.setText(R.string.Connecting)
        } else {
            progressBar.isVisible = false
            stateTv.setText(R.string.Network_unavailable)
        }
        stateLayout.setOnClickListener {
            DiagnosisActivity.show(requireContext())
        }
    }

    private fun setSyncing() {
        progressBar.visibility = VISIBLE
        stateLayout.setBackgroundResource(R.color.stateGreen)
        stateTv.setText(R.string.Syncing_messages)
        stateLayout.setOnClickListener(null)
    }
}
