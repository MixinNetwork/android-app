@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

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
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.ui.TimeBar
import one.mixin.android.R
import one.mixin.android.databinding.ViewPlayerControlBinding
import one.mixin.android.extension.dp
import one.mixin.android.util.VideoPlayer
import one.mixin.android.extension.round
import one.mixin.android.extension.isLandscape
import one.mixin.android.widget.PlayView2.Companion.STATUS_IDLE
import one.mixin.android.widget.PlayView2.Companion.STATUS_PLAYING
import java.util.Formatter
import java.util.Locale
import one.mixin.android.util.video.VideoPlayerControls
import kotlin.math.min

@UnstableApi class PlayerControlView(context: Context, attributeSet: AttributeSet) :
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

    private val updateProgressAction =
        Runnable {
            updateProgress()
        }
    private val hideAction =
        Runnable {
            hide()
        }

    var useBuiltInTopBar = true
        set(value) {
            field = value
            topLayout.isVisible = value
            ViewCompat.requestApplyInsets(this)
        }
    var liveSource = false
    var onVideoStateChanged: (() -> Unit)? = null
    val isLive get() = liveSource || player?.isCurrentMediaItemDynamic == true
    private var previewLoader: VideoFramePreviewLoader? = null
    var videoPath: String? = null
        set(value) {
            if (field == value) return
            field = value
            previewLoader?.setVideoSource(value)
            hidePreview()
        }

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
            if (attachedToWindow) value?.addListener(componentListener)
            updateAll()
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
    private val bottomLayout by lazy { binding.bottomLl }
    val fullscreenIv by lazy { binding.fullscreenIv }
    val pipView by lazy { binding.pipIv }
    val closeIv by lazy { binding.closeIv }

    init {
        binding.scrubberPreviewIv.round(6.dp)
        binding.muteIv.setOnClickListener {
            if (player == null) return@setOnClickListener
            VideoPlayer.player().toggleMute()
            updatePlaybackControls()
            hideAfterTimeout()
        }
        binding.speedTv.setOnClickListener {
            if (player == null || isLive) return@setOnClickListener
            val video = VideoPlayer.player()
            video.setSpeed(VideoPlayerControls.nextSpeed(video.getSpeed()))
            updatePlaybackControls()
            hideAfterTimeout()
        }
        playView.setOnClickListener(componentListener)
        timeBar.addListener(componentListener)
        ViewCompat.setOnApplyWindowInsetsListener(this) { _: View?, insets: WindowInsetsCompat ->
            insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()).let { safeInsets ->
                if (context.isLandscape()) {
                    binding.topFl.setPadding(safeInsets.left, safeInsets.top.takeIf { useBuiltInTopBar } ?: 0, safeInsets.right, 0)
                    binding.bottomLl.setPadding(
                        12.dp + safeInsets.left,
                        12.dp,
                        12.dp + safeInsets.right,
                        24.dp + safeInsets.bottom,
                    )
                } else {
                    binding.topFl.setPadding(safeInsets.left, if (useBuiltInTopBar) 24.dp + safeInsets.top else 0, safeInsets.right, 0)
                    binding.bottomLl.setPadding(
                        12.dp + safeInsets.left,
                        24.dp,
                        12.dp + safeInsets.right,
                        24.dp + safeInsets.bottom,
                    )
                }
                insets
            }
        }
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ViewCompat.requestApplyInsets(this)
        attachedToWindow = true
        player?.addListener(componentListener)
        updateAll()
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
        hidePreview()
        previewLoader?.release()
        previewLoader = null
        player?.removeListener(componentListener)
        removeCallbacks(updateProgressAction)
        removeCallbacks(hideAction)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            removeCallbacks(hideAction)
        } else if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            hideAfterTimeout()
        }
        return super.dispatchTouchEvent(ev)
    }

    fun show() {
        if (!isVisible) {
            visibility = View.VISIBLE
            visibilityListener?.onVisibilityChange(visibility)
            playView.requestFocus()
        }
        updateAll()
        hideAfterTimeout()
    }

    fun hide() {
        if (isVisible) {
            hidePreview()
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
        } else {
            fullscreenIv.setImageResource(R.drawable.ic_fullscreen)
        }
    }

    fun updateLiveView() {
        liveView.isVisible = isLive
        binding.timeRow.isVisible = !isLive
        binding.speedTv.isVisible = !isLive
        onVideoStateChanged?.invoke()
    }

    private fun updatePlaybackControls() {
        val muted = player?.volume == 0f
        binding.muteIv.setImageResource(if (muted) R.drawable.ic_player_volume_off else R.drawable.ic_player_volume_on)
        binding.muteIv.contentDescription = context.getString(if (muted) R.string.Unmute else R.string.Mute)
        val speed = player?.playbackParameters?.speed ?: 1f
        binding.speedTv.text = speed.toString().removeSuffix(".0") + "x"
        switchFullscreen(context.isLandscape())
    }

    private fun showPreview(position: Long) {
        val time = Util.getStringForTime(formatBuilder, formatter, position)
        positionView.text = time
        binding.scrubberPreviewTimeTv.text = time
        val bubble = binding.scrubberPreviewContainer
        bubble.isVisible = true
        val duration = player?.duration?.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        val fraction = if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
        val visualFraction = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) 1f - fraction else fraction
        val center = binding.timeRow.left + timeBar.left + timeBar.width * visualFraction
        fun placeBubble() {
            bubble.doOnLayout {
                val maxX = (bottomLayout.width - bubble.width).coerceAtLeast(0).toFloat()
                bubble.x = (center - bubble.width / 2f).coerceIn(0f, maxX)
            }
        }
        placeBubble()
        val loader = previewLoader ?: VideoFramePreviewLoader().also {
            it.setVideoSource(videoPath)
            previewLoader = it
        }
        loader.loadFrame(position) { frame ->
            if (scrubbing) {
                binding.scrubberPreviewIv.setImageBitmap(frame)
                binding.scrubberPreviewIv.isVisible = frame != null
                if (frame != null) {
                    val maxWidth = 136.dp
                    val maxHeight = 110.dp
                    val scale = minOf(maxWidth.toFloat() / frame.width, maxHeight.toFloat() / frame.height)
                    binding.scrubberPreviewIv.updateLayoutParams {
                        width = (frame.width * scale).toInt().coerceAtLeast(1)
                        height = (frame.height * scale).toInt().coerceAtLeast(1)
                    }
                }
                placeBubble()
            }
        }
    }

    private fun hidePreview() {
        if (scrubbing) parent?.requestDisallowInterceptTouchEvent(false)
        scrubbing = false
        binding.scrubberPreviewContainer.isVisible = false
        binding.scrubberPreviewIv.isVisible = false
        binding.scrubberPreviewIv.setImageDrawable(null)
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

    private fun seekTo(
        player: Player,
        windowIndex: Int,
        positionMs: Long,
    ): Boolean {
        player.seekTo(windowIndex, positionMs)
        return true
    }

    private fun seekToTimeBarPosition(
        player: Player,
        positionMsParams: Long,
    ) {
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
        topLayout.isVisible = useBuiltInTopBar
        updateLiveView()
        updatePlaybackControls()
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
            Player.STATE_BUFFERING -> {
                // Do nothing
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
            extraAdGroupCount,
        )
        System.arraycopy(
            extraPlayedAdGroups,
            0,
            playedAdGroups,
            adGroupCount,
            extraAdGroupCount,
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
            delayMs =
                Util.constrainValue(
                    delayMs,
                    timeBarMinUpdateIntervalMs.toLong(),
                    MAX_UPDATE_INTERVAL_MS,
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
        override fun onScrubMove(
            timeBar: TimeBar,
            position: Long,
        ) {
            showPreview(position)
        }

        override fun onScrubStart(
            timeBar: TimeBar,
            position: Long,
        ) {
            scrubbing = true
            removeCallbacks(hideAction)
            parent?.requestDisallowInterceptTouchEvent(true)
            showPreview(position)
        }

        override fun onScrubStop(
            timeBar: TimeBar,
            position: Long,
            canceled: Boolean,
        ) {
            hidePreview()
            hideAfterTimeout()
            if (!canceled) {
                player?.let { seekToTimeBarPosition(it, position) }
            }
        }

        override fun onPlayerStateChanged(
            playWhenReady: Boolean,
            playbackState: Int,
        ) {
            onVideoStateChanged?.invoke()
            updatePlayView()
            updateProgress()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateProgress()
        }

        override fun onPositionDiscontinuity(
            @Player.DiscontinuityReason reason: Int,
        ) {
            updateNavigation()
            updateTimeline()
        }

        override fun onTimelineChanged(
            timeline: Timeline,
            reason: Int,
        ) {
            updateLiveView()
            updateNavigation()
            updateTimeline()
        }

        override fun onVolumeChanged(volume: Float) = updatePlaybackControls()

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = updatePlaybackControls()

        override fun onClick(v: View) {
            hideAfterTimeout()
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
        fun onProgressUpdate(
            position: Long,
            bufferedPosition: Long,
        )
    }

    companion object {
        private const val MAX_UPDATE_INTERVAL_MS = 1000L
        private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000L
        private const val MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100
        private const val DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS = 200

        const val DEFAULT_SHOW_TIMEOUT_MS = 5000

        fun canShowMultiWindowTimeBar(
            timeline: Timeline,
            window: Timeline.Window,
        ): Boolean {
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
