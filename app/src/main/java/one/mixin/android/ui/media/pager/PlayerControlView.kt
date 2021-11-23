package one.mixin.android.ui.media.pager

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Util
import one.mixin.android.R
import one.mixin.android.databinding.ViewPlayerControlBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.isLandscape
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.widget.PlayView2.Companion.STATUS_IDLE
import one.mixin.android.widget.PlayView2.Companion.STATUS_PLAYING
import java.util.Formatter
import java.util.Locale
import kotlin.math.min

class PlayerControlView(context: Context, attributeSet: AttributeSet) :
    FrameLayout(context, attributeSet) {

    private val componentListener = ComponentListener()
    private val formatBuilder = StringBuilder()
    private val formatter = Formatter(formatBuilder, Locale.getDefault())
    private val window = Timeline.Window()
    private val period = Timeline.Period()
    private var adGroupTimesMs = longArrayOf()
    private var playedAdGroups = booleanArrayOf()
    private var extraAdGroupTimesMs = longArrayOf()
    private var extraPlayedAdGroups = booleanArrayOf()

    private var showMultiWindowTimeBar: Boolean = false
    private var multiWindowTimeBar: Boolean = false
    private var currentWindowOffset = 0L
    private var timeBarMinUpdateIntervalMs = DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS
    private var attachedToWindow = false
    private var hideAtMs = C.TIME_UNSET

    private val statusBarHeight = context.statusBarHeight()

    private val updateProgressAction = Runnable {
        updateProgress()
    }
    private val hideAction = Runnable {
        hide()
    }

    private var useBottomLayout = false
    var inRefreshState = false
        set(value) {
            field = value
            updateAll()
        }

    var showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS
    var scrubbing = false

    var preparePlayback: (() -> Unit)? = null

    var player: Player? = null
        set(value) {
            field?.apply {
                removeListener(componentListener)
            }
            field = value
            value?.apply {
                addListener(componentListener)
            }
        }

    var messageId: String? = null

    var progressUpdateListener: ProgressUpdateListener? = null
    var visibilityListener: VisibilityListener? = null

    private val binding = ViewPlayerControlBinding.inflate(LayoutInflater.from(context), this, true)
    private val topLayout by lazy { binding.topFl }
    private val playView by lazy { binding.playView }
    private val durationView by lazy { binding.exoDuration }
    private val positionView by lazy { binding.exoPosition }
    private val timeBar by lazy { binding.exoProgress }
    private val liveView by lazy { binding.liveTv }
    val bottomLayout by lazy { binding.bottomLl }
    val fullscreenIv by lazy { binding.fullscreenIv }
    val pipView by lazy { binding.pipIv }
    val closeIv by lazy { binding.closeIv }

    init {
        playView.setOnClickListener(componentListener)
        timeBar.addListener(componentListener)
        ViewCompat.setOnApplyWindowInsetsListener(this) { _: View?, insets: WindowInsetsCompat ->
            insets.getInsets(WindowInsetsCompat.Type.systemBars()).let { systemInserts ->
                if (context.isLandscape()) {
                    binding.topFl.setPadding(systemInserts.left, 0, systemInserts.right, 0)
                    binding.bottomLl.setPadding(
                        12.dp + systemInserts.left,
                        12.dp,
                        12.dp + systemInserts.right,
                        24.dp
                    )
                } else {
                    binding.topFl.setPadding(0, 24.dp + systemInserts.top, 0, 0)
                    binding.bottomLl.setPadding(
                        12.dp,
                        24.dp,
                        12.dp,
                        24.dp + systemInserts.bottom
                    )
                }
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        topLayout.setPadding(0, statusBarHeight, 0, 0)
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true
        player?.addListener(componentListener)
        if (hideAtMs != C.TIME_UNSET) {
            val delayMs = hideAtMs - SystemClock.uptimeMillis()
            if (delayMs <= 0) {
                hide()
            } else {
                postDelayed(hideAction, delayMs)
            }
        } else if (isVisible) {
            hideAfterTimeout()
        }
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attachedToWindow = false
        player?.removeListener(componentListener)
        removeCallbacks(updateProgressAction)
        removeCallbacks(hideAction)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            removeCallbacks(hideAction)
        } else if (ev.action == MotionEvent.ACTION_UP) {
            hideAfterTimeout()
        }
        return super.dispatchTouchEvent(ev)
    }

    fun show() {
        if (!isVisible) {
            visibility = View.VISIBLE
            visibilityListener?.onVisibilityChange(visibility)
            updateAll()
            playView.requestFocus()
        }
        hideAfterTimeout()
    }

    fun hide() {
        if (isVisible) {
            visibility = View.GONE
            visibilityListener?.onVisibilityChange(visibility)
            removeCallbacks(updateProgressAction)
            removeCallbacks(hideAction)
            hideAtMs = C.TIME_UNSET
        }
    }

    fun switchFullscreen(fullscreen: Boolean) {
        if (fullscreen) {
            fullscreenIv.setImageResource(R.drawable.ic_fullscreen_exit)
            topLayout.setPadding(0, 0, 0, 0)
        } else {
            fullscreenIv.setImageResource(R.drawable.ic_fullscreen)
            topLayout.setPadding(0, statusBarHeight, 0, 0)
        }
    }

    fun updateLiveView() {
        // TODO support multiWindowTimeBar
        player?.let {
            val timeLine = it.currentTimeline
            val windowIndex = it.currentWindowIndex
            try {
                val currentWindow = timeLine.getWindow(windowIndex, window)
                liveView.isVisible = currentWindow.isDynamic
                useBottomLayout = !currentWindow.isDynamic
            } catch (ignored: IndexOutOfBoundsException) {
            }
        }
    }

    private fun hideAfterTimeout() {
        removeCallbacks(hideAction)
        if (showTimeoutMs > 0) {
            hideAtMs = SystemClock.uptimeMillis() + showTimeoutMs
            if (isAttachedToWindow) {
                postDelayed(hideAction, showTimeoutMs.toLong())
            }
        } else {
            hideAtMs = C.TIME_UNSET
        }
    }

    private fun seekTo(player: Player, windowIndex: Int, positionMs: Long): Boolean {
        player.seekTo(windowIndex, positionMs)
        return true
    }

    private fun seekToTimeBarPosition(player: Player, positionMsParams: Long) {
        var positionMs = positionMsParams
        var windowIndex: Int
        val timeline = player.currentTimeline
        if (multiWindowTimeBar && !timeline.isEmpty) {
            val windowCount = timeline.windowCount
            windowIndex = 0
            while (true) {
                val windowDurationMs = timeline.getWindow(windowIndex, window).durationMs
                if (positionMs < windowDurationMs) {
                    break
                } else if (windowIndex == windowCount - 1) {
                    // Seeking past the end of the last window should seek to the end of the timeline.
                    positionMs = windowDurationMs
                    break
                }
                positionMs -= windowDurationMs
                windowIndex++
            }
        } else {
            windowIndex = player.currentWindowIndex
        }
        val dispatched = seekTo(player, windowIndex, positionMs)
        if (!dispatched) {
            // The seek wasn't dispatched then the progress bar scrubber will be in the wrong position.
            // Trigger a progress update to snap it back.
            updateProgress()
        }
    }

    private fun updateAll() {
        updatePlayView()
        updateNavigation()
        updateTimeline()
        if (!topLayout.isVisible) {
            topLayout.isVisible = true
        }
        if (useBottomLayout) {
            if (!bottomLayout.isVisible) {
                bottomLayout.isVisible = true
            }
        } else {
            bottomLayout.isVisible = false
        }
    }

    private fun updatePlayView() {
        if (!isVisible || !isAttachedToWindow) {
            return
        }
        val player = this.player ?: return
        playView.isVisible = !inRefreshState

        when (player.playbackState) {
            Player.STATE_IDLE -> {
                playView.status = STATUS_IDLE
            }
            Player.STATE_READY -> {
                if (player.playWhenReady) {
                    playView.status = STATUS_PLAYING
                } else {
                    playView.status = STATUS_IDLE
                }
            }
            Player.STATE_ENDED -> {
                playView.status = STATUS_IDLE
            }
        }
    }

    private fun updateNavigation() {
        if (!isVisible || !isAttachedToWindow) {
            return
        }
        var enableSeeking = false
        if (player != null) {
            val timeline = player!!.currentTimeline
            if (!timeline.isEmpty && !player!!.isPlayingAd) {
                timeline.getWindow(player!!.currentWindowIndex, window)
                val isSeekable = window.isSeekable
                enableSeeking = isSeekable
            }
        }
        timeBar.isEnabled = enableSeeking
    }

    private fun updateTimeline() {
        if (player == null) {
            return
        }
        multiWindowTimeBar =
            showMultiWindowTimeBar && canShowMultiWindowTimeBar(player!!.currentTimeline, window)
        currentWindowOffset = 0
        var durationUs: Long = 0
        var adGroupCount = 0
        val timeline = player!!.currentTimeline
        if (!timeline.isEmpty) {
            val currentWindowIndex = player!!.currentWindowIndex
            val firstWindowIndex = if (multiWindowTimeBar) 0 else currentWindowIndex
            val lastWindowIndex =
                if (multiWindowTimeBar) timeline.windowCount - 1 else currentWindowIndex
            for (i in firstWindowIndex..lastWindowIndex) {
                if (i == currentWindowIndex) {
                    currentWindowOffset = C.usToMs(durationUs)
                }
                timeline.getWindow(i, window)
                if (window.durationUs == C.TIME_UNSET) {
                    Assertions.checkState(!multiWindowTimeBar)
                    break
                }
                for (j in window.firstPeriodIndex..window.lastPeriodIndex) {
                    timeline.getPeriod(j, period)
                    val periodAdGroupCount = period.adGroupCount
                    for (adGroupIndex in 0 until periodAdGroupCount) {
                        var adGroupTimeInPeriodUs = period.getAdGroupTimeUs(adGroupIndex)
                        if (adGroupTimeInPeriodUs == C.TIME_END_OF_SOURCE) {
                            if (period.durationUs == C.TIME_UNSET) {
                                // Don't show ad markers for postrolls in periods with unknown duration.
                                continue
                            }
                            adGroupTimeInPeriodUs = period.durationUs
                        }
                        val adGroupTimeInWindowUs =
                            adGroupTimeInPeriodUs + period.positionInWindowUs
                        if (adGroupTimeInWindowUs >= 0 && adGroupTimeInWindowUs <= window.durationUs) {
                            if (adGroupCount == adGroupTimesMs.size) {
                                val newLength =
                                    if (adGroupTimesMs.isEmpty()) 1 else adGroupTimesMs.size * 2
                                adGroupTimesMs = adGroupTimesMs.copyOf(newLength)
                                playedAdGroups = playedAdGroups.copyOf(newLength)
                            }
                            adGroupTimesMs[adGroupCount] =
                                C.usToMs(durationUs + adGroupTimeInWindowUs)
                            playedAdGroups[adGroupCount] = period.hasPlayedAdGroup(adGroupIndex)
                            adGroupCount++
                        }
                    }
                }
                durationUs += window.durationUs
            }
        }
        val durationMs = C.usToMs(durationUs)
        durationView.text = Util.getStringForTime(formatBuilder, formatter, durationMs)
        timeBar.setDuration(durationMs)
        val extraAdGroupCount = extraAdGroupTimesMs.size
        val totalAdGroupCount = adGroupCount + extraAdGroupCount
        if (totalAdGroupCount > adGroupTimesMs.size) {
            adGroupTimesMs = adGroupTimesMs.copyOf(totalAdGroupCount)
            playedAdGroups = playedAdGroups.copyOf(totalAdGroupCount)
        }
        System.arraycopy(
            extraAdGroupTimesMs,
            0,
            adGroupTimesMs,
            adGroupCount,
            extraAdGroupCount
        )
        System.arraycopy(
            extraPlayedAdGroups,
            0,
            playedAdGroups,
            adGroupCount,
            extraAdGroupCount
        )
        timeBar.setAdGroupTimesMs(adGroupTimesMs, playedAdGroups, totalAdGroupCount)
        updateProgress()
    }

    private fun updateProgress() {
        if (!isVisible || !isAttachedToWindow) {
            return
        }

        var position: Long = 0
        var bufferedPosition: Long = 0
        if (player != null) {
            position = currentWindowOffset + player!!.contentPosition
            bufferedPosition = currentWindowOffset + player!!.contentBufferedPosition
        }
        if (!scrubbing) {
            positionView.text = Util.getStringForTime(formatBuilder, formatter, position)
        }
        timeBar.setPosition(position)
        timeBar.setBufferedPosition(bufferedPosition)
        progressUpdateListener?.onProgressUpdate(position, bufferedPosition)

        // Cancel any pending updates and schedule a new one if necessary.
        removeCallbacks(updateProgressAction)
        val playbackState = if (player == null) Player.STATE_IDLE else player!!.playbackState
        if (player != null && player!!.isPlaying) {
            var mediaTimeDelayMs = timeBar.preferredUpdateDelay

            // Limit delay to the start of the next full second to ensure position display is smooth.
            val mediaTimeUntilNextFullSecondMs = 1000 - position % 1000
            mediaTimeDelayMs = min(mediaTimeDelayMs, mediaTimeUntilNextFullSecondMs)

            // Calculate the delay until the next update in real time, taking playbackSpeed into account.
            val playbackSpeed = player!!.playbackParameters.speed
            var delayMs =
                if (playbackSpeed > 0) (mediaTimeDelayMs / playbackSpeed).toLong() else MAX_UPDATE_INTERVAL_MS

            // Constrain the delay to avoid too frequent / infrequent updates.
            delayMs = Util.constrainValue(
                delayMs,
                timeBarMinUpdateIntervalMs.toLong(),
                MAX_UPDATE_INTERVAL_MS
            )
            postDelayed(updateProgressAction, delayMs)
        } else if (playbackState != Player.STATE_ENDED && playbackState != Player.STATE_IDLE) {
            postDelayed(updateProgressAction, MAX_UPDATE_INTERVAL_MS)
        }
    }

    inner class ComponentListener :
        Player.Listener,
        TimeBar.OnScrubListener,
        OnClickListener {
        override fun onScrubMove(timeBar: TimeBar, position: Long) {
            positionView.text = Util.getStringForTime(formatBuilder, formatter, position)
        }

        override fun onScrubStart(timeBar: TimeBar, position: Long) {
            scrubbing = true
            positionView.text = Util.getStringForTime(formatBuilder, formatter, position)
        }

        override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
            scrubbing = false
            if (!canceled) {
                player?.let { seekToTimeBarPosition(it, position) }
            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            updatePlayView()
            updateProgress()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateProgress()
        }

        override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
            updateNavigation()
            updateTimeline()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            updateNavigation()
            updateTimeline()
        }

        override fun onClick(v: View) {
            val player = this@PlayerControlView.player ?: return

            if (v == playView) {
                when (playView.status) {
                    STATUS_IDLE -> {
                        if (player.playbackState == Player.STATE_IDLE) {
                            player.prepare()
                            preparePlayback?.invoke()
                        } else if (player.playbackState == Player.STATE_ENDED) {
                            seekTo(player, player.currentWindowIndex, C.TIME_UNSET)
                        }
                        player.playWhenReady = true
                    }
                    STATUS_PLAYING -> {
                        player.playWhenReady = false
                    }
                }
            }
        }
    }

    interface VisibilityListener {

        fun onVisibilityChange(visibility: Int)
    }

    interface ProgressUpdateListener {

        fun onProgressUpdate(position: Long, bufferedPosition: Long)
    }

    companion object {
        private const val MAX_UPDATE_INTERVAL_MS = 1000L
        private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000L
        private const val MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100
        private const val DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS = 200

        const val DEFAULT_SHOW_TIMEOUT_MS = 5000

        fun canShowMultiWindowTimeBar(timeline: Timeline, window: Timeline.Window): Boolean {
            if (timeline.windowCount > MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR) {
                return false
            }
            for (i in 0..timeline.windowCount) {
                if (timeline.getWindow(i, window).durationUs == C.TIME_UNSET) {
                    return false
                }
            }
            return true
        }
    }
}
