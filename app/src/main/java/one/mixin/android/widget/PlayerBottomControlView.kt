package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import one.mixin.android.R
import one.mixin.android.databinding.ViewPlayerBottomControlBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.widget.PlayView2.Companion.STATUS_IDLE
import one.mixin.android.widget.PlayView2.Companion.STATUS_PLAYING
import org.jetbrains.anko.textColorResource
import java.util.Formatter
import java.util.Locale
import kotlin.math.min

class PlayerBottomControlView(context: Context, attributeSet: AttributeSet) :
    FrameLayout(context, attributeSet) {

    private val componentListener = ComponentListener()
    private val controlDispatcher = DefaultControlDispatcher()
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

    private val updateProgressAction = Runnable {
        updateProgress()
    }

    var inRefreshState = false
        set(value) {
            field = value
            updateAll()
        }

    var scrubbing = false

    var player: Player? = null
        set(value) {
            field?.apply {
                removeListener(componentListener)
            }
            field = value
            value?.apply {
                addListener(componentListener)
            }
            updateAll()
        }

    var messageId: String? = null

    private var playMode: PlayMode = PlayMode.RepeatAll
    private var playSpeed: PlaySpeed = PlaySpeed.Speed1

    var progressUpdateListener: ProgressUpdateListener? = null

    private val binding = ViewPlayerBottomControlBinding.inflate(LayoutInflater.from(context), this, true)
    private val playView by lazy { binding.playView }
    private val durationView by lazy { binding.durationTv }
    private val positionView by lazy { binding.positionTv }
    private val timeBar by lazy { binding.progressTv }
    private val modeView by lazy { binding.modeIb }
    private val previousView by lazy { binding.previousIb }
    private val nextView by lazy { binding.nextIb }
    private val speedView by lazy { binding.speedTv }

    init {
        playView.setOnClickListener(componentListener)
        playView.setColor(context.colorFromAttribute(R.attr.icon_default), context.colorFromAttribute(R.attr.icon_white))
        modeView.setOnClickListener(componentListener)
        previousView.setOnClickListener(componentListener)
        nextView.setOnClickListener(componentListener)
        speedView.setOnClickListener(componentListener)
        timeBar.addListener(componentListener)
    }

    private fun seekTo(player: Player, windowIndex: Int, positionMs: Long): Boolean {
        return controlDispatcher.dispatchSeekTo(player, windowIndex, positionMs)
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
    }

    private fun updatePlayView() {
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
        post {
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
    }

    private fun updateProgress() {
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
        Player.EventListener,
        TimeBar.OnScrubListener,
        OnClickListener,
        VideoListener {
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

        @SuppressLint("SetTextI18n")
        override fun onClick(v: View) {
            val player = this@PlayerBottomControlView.player ?: return

            when (v) {
                playView -> {
                    when (playView.status) {
                        STATUS_IDLE -> {
                            if (player.playbackState == Player.STATE_IDLE) {
                                controlDispatcher.dispatchPrepare(player)
                            } else if (player.playbackState == Player.STATE_ENDED) {
                                seekTo(player, player.currentWindowIndex, C.TIME_UNSET)
                            }
                            controlDispatcher.dispatchSetPlayWhenReady(player, true)
                        }
                        STATUS_PLAYING -> {
                            controlDispatcher.dispatchSetPlayWhenReady(player, false)
                        }
                    }
                }
                modeView -> {
                    when (playMode) {
                        PlayMode.RepeatAll -> {
                            controlDispatcher.dispatchSetShuffleModeEnabled(player, false)
                            controlDispatcher.dispatchSetRepeatMode(player, Player.REPEAT_MODE_ONE)
                            modeView.setImageResource(R.drawable.ic_player_repeat_one)
                            playMode = PlayMode.RepeatOne
                        }
                        PlayMode.RepeatOne -> {
                            controlDispatcher.dispatchSetShuffleModeEnabled(player, true)
                            modeView.setImageResource(R.drawable.ic_player_shuffle)
                            playMode = PlayMode.Shuffle
                        }
                        PlayMode.Shuffle -> {
                            controlDispatcher.dispatchSetShuffleModeEnabled(player, false)
                            controlDispatcher.dispatchSetRepeatMode(player, Player.REPEAT_MODE_ALL)
                            modeView.setImageResource(R.drawable.ic_player_repeat_all)
                            playMode = PlayMode.RepeatAll
                        }
                    }
                }
                nextView -> {
                    controlDispatcher.dispatchNext(player)
                }
                previousView -> {
                    controlDispatcher.dispatchPrevious(player)
                }
                speedView -> {
                    when (playSpeed) {
                        PlaySpeed.Speed1 -> {
                            player.setPlaybackParameters(PlaybackParameters(1.5f))
                            speedView.text = "1.5X"
                            speedView.textColorResource = R.color.colorAccent
                            playSpeed = PlaySpeed.Speed15
                        }
                        PlaySpeed.Speed15 -> {
                            player.setPlaybackParameters(PlaybackParameters(2.0f))
                            speedView.text = "2.0X"
                            speedView.textColorResource = R.color.colorAccentNight
                            playSpeed = PlaySpeed.Speed20
                        }
                        PlaySpeed.Speed20 -> {
                            player.setPlaybackParameters(PlaybackParameters(1f))
                            speedView.text = "1X"
                            speedView.setTextColor(R.attr.icon_default)
                            playSpeed = PlaySpeed.Speed1
                        }
                    }
                }
            }
        }
    }

    interface ProgressUpdateListener {

        fun onProgressUpdate(position: Long, bufferedPosition: Long)
    }

    companion object {
        private const val MAX_UPDATE_INTERVAL_MS = 1000L
        private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000L
        private const val MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100
        private const val DEFAULT_TIME_BAR_MIN_UPDATE_INTERVAL_MS = 200

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

enum class PlayMode {
    RepeatAll, RepeatOne, Shuffle
}

enum class PlaySpeed {
    Speed1, Speed15, Speed20
}
