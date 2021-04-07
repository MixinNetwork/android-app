package one.mixin.android.util.video

import android.annotation.SuppressLint
import android.view.TextureView
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import okhttp3.OkHttpClient
import okhttp3.Request
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.util.reportExoPlayerException
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
class MixinPlayer(val isAudio: Boolean = false) : Player.EventListener, VideoListener {

    val player: SimpleExoPlayer by lazy {
        val trackSelector = if (isAudio) {
            DefaultTrackSelector(MixinApplication.appContext)
        } else {
            DefaultTrackSelector(MixinApplication.appContext, AdaptiveTrackSelection.Factory())
        }
        SimpleExoPlayer.Builder(MixinApplication.appContext)
            .setTrackSelector(trackSelector)
            .build().apply {
                volume = 1.0f
                addListener(this@MixinPlayer)
                addVideoListener(this@MixinPlayer)
            }
    }
    private var onVideoPlayerListener: OnVideoPlayerListener? = null
    private var onMediaPlayerListener: OnMediaPlayerListener? = null
    private var mediaSource: MediaSource? = null
    private var cycle = true

    fun setCycle(cycle: Boolean) {
        this.cycle = cycle
    }

    fun setAudioStreamType(streamType: Int) {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(streamType)
            .build()
        this.player.setAudioAttributes(audioAttributes, false)
        player.volume = 1f
    }

    fun isPlaying() = player.playWhenReady

    fun duration() = if (player.duration == C.TIME_UNSET) 0 else player.duration.toInt()

    fun videoFormat() = player.videoFormat

    fun currentPosition() = player.currentPosition

    private val period = Timeline.Period()

    fun getCurrentPos(): Long {
        var position = player.currentPosition
        val currentTimeline = player.currentTimeline
        if (!currentTimeline.isEmpty) {
            position -= currentTimeline.getPeriod(player.currentPeriodIndex, period)
                .positionInWindowMs
        }
        return position
    }

    fun release() = player.release()

    fun stop() = player.stop()

    fun start() {
        if (!player.playWhenReady) {
            player.playWhenReady = true
        }
    }

    fun pause() {
        if (player.playWhenReady) {
            player.playWhenReady = false
        }
    }

    fun seekTo(pos: Long) {
        player.seekTo(pos)
    }

    fun seekTo(timeMillis: Int) {
        val seekPos = (
            if (player.duration == C.TIME_UNSET)
                0
            else min(max(0, timeMillis), duration())
            ).toLong()
        seekTo(seekPos)
    }

    fun setVolume(volume: Float) {
        player.volume = volume
    }

    fun setVideoTextureView(texture: TextureView) {
        player.setVideoTextureView(texture)
    }

    private var url: String? = null

    fun loadVideo(url: String, force: Boolean = false) {
        if (!force && this.url == url && (player.playbackState == STATE_READY || player.playbackState == STATE_BUFFERING)) {
            return
        }
        this.url = url
        mediaSource = ProgressiveMediaSource.Factory(buildDataSourceFactory())
            .createMediaSource(url.toMediaItem()).apply {
                player.setMediaSource(this)
                player.prepare()
            }
    }

    fun loadVideo(url: String, id: String, force: Boolean = false) {
        if (!force && this.url == url && (player.playbackState == STATE_READY || player.playbackState == STATE_BUFFERING)) {
            return
        }
        this.mId = id
        this.url = url
        mediaSource = ProgressiveMediaSource.Factory(buildDataSourceFactory())
            .createMediaSource(url.toMediaItem()).apply {
                player.setMediaSource(this)
                player.prepare()
            }
    }

    fun loadAudio(url: String) {
        mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(MixinApplication.appContext, BuildConfig.APPLICATION_ID))
            .createMediaSource(url.toMediaItem()).apply {
                player.setMediaSource(this)
                player.prepare()
            }
    }

    private fun String.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setUri(this)
            .build()

    var mId: String? = null

    fun loadHlsVideo(url: String, id: String, force: Boolean = false) {
        if (!force && this.url == url && (player.playbackState == STATE_READY || player.playbackState == STATE_BUFFERING)) {
            return
        }
        this.mId = id
        this.url = url
        doAsync {
            var contentType = "application/x-mpegURL"
            try {
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                response.header("Content-Type")?.let {
                    contentType = it
                }
            } catch (e: Exception) {
            }
            uiThread {
                mediaSource = if (contentType.contains("application/x-mpegURL", true) ||
                    contentType.contains("application/vnd.apple.mpegurl", true) ||
                    contentType.contains("binary/octet-stream", ignoreCase = true)
                ) {
                    HlsMediaSource.Factory(buildDataSourceFactory()).createMediaSource(url.toMediaItem())
                } else {
                    ProgressiveMediaSource.Factory(buildDataSourceFactory()).createMediaSource(url.toMediaItem())
                }.apply {
                    player.setMediaSource(this)
                    player.prepare()
                }
            }
        }
    }

    private fun buildDataSourceFactory(): DataSource.Factory {
        return DefaultDataSourceFactory(
            MixinApplication.appContext,
            Util.getUserAgent(MixinApplication.appContext, "Mixin")
        )
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
        onVideoPlayerListener?.onTracksChanged(trackGroups, trackSelections)
        mId?.let {
            onMediaPlayerListener?.onTracksChanged(it, trackGroups, trackSelections)
        }
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        onVideoPlayerListener?.onLoadingChanged(isLoading)
        mId?.let {
            onMediaPlayerListener?.onLoadingChanged(it, isLoading)
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        onVideoPlayerListener?.onPlayerStateChanged(playWhenReady, playbackState)
        mId?.let {
            onMediaPlayerListener?.onPlayerStateChanged(it, playWhenReady, playbackState)
        }
        if (cycle && playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        if (isBehindLiveWindow(error)) {
            mediaSource?.let {
                player.setMediaSource(it)
                player.prepare()
            }
        }
        // HttpDataSourceException
        onVideoPlayerListener?.onPlayerError(error)
        mId?.let {
            onMediaPlayerListener?.onPlayerError(it, error)
        }

        reportExoPlayerException("MixinPlayer", error)
    }

    override fun onPositionDiscontinuity(reason: Int) {
        onVideoPlayerListener?.onPositionDiscontinuity()
        mId?.let {
            onMediaPlayerListener?.onPositionDiscontinuity(it)
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        onVideoPlayerListener?.onPlaybackParametersChanged(playbackParameters)
        mId?.let {
            onMediaPlayerListener?.onPlaybackParametersChanged(it, playbackParameters)
        }
    }

    override fun onSeekProcessed() {
    }

    override fun onVideoSizeChanged(
        width: Int,
        height: Int,
        unappliedRotationDegrees: Int,
        pixelWidthHeightRatio: Float
    ) {
        onVideoPlayerListener?.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
        mId?.let {
            onMediaPlayerListener?.onVideoSizeChanged(it, width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
        }
    }

    override fun onRenderedFirstFrame() {
        onVideoPlayerListener?.onRenderedFirstFrame()
        mId?.let {
            onMediaPlayerListener?.onRenderedFirstFrame(it)
        }
    }

    fun setOnVideoPlayerListener(onVideoPlayerListener: OnVideoPlayerListener?) {
        this.onVideoPlayerListener = onVideoPlayerListener
    }

    fun setOnMediaPlayerListener(onMediaPlayerListener: OnMediaPlayerListener?) {
        this.onMediaPlayerListener = onMediaPlayerListener
    }

    fun setSpeed(speed: Float) {
        val pp = PlaybackParameters(speed, player.playbackParameters.pitch)
        player.setPlaybackParameters(pp)
    }

    interface OnVideoPlayerListener {
        fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float)

        fun onRenderedFirstFrame()

        fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int)

        fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters)

        fun onPositionDiscontinuity()

        fun onLoadingChanged(isLoading: Boolean)

        fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray)

        fun onTimelineChanged(timeline: Timeline, manifest: Any)

        fun onPlayerError(error: ExoPlaybackException)
    }

    open class VideoPlayerListenerWrapper : OnVideoPlayerListener {

        override fun onVideoSizeChanged(
            width: Int,
            height: Int,
            unappliedRotationDegrees: Int,
            pixelWidthHeightRatio: Float
        ) {
        }

        override fun onRenderedFirstFrame() {}

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}

        override fun onPositionDiscontinuity() {}

        override fun onLoadingChanged(isLoading: Boolean) {}

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

        override fun onTimelineChanged(timeline: Timeline, manifest: Any) {}

        override fun onPlayerError(error: ExoPlaybackException) {}
    }

    interface OnMediaPlayerListener {
        fun onVideoSizeChanged(mid: String, width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float)

        fun onRenderedFirstFrame(mid: String)

        fun onPlayerStateChanged(mid: String, playWhenReady: Boolean, playbackState: Int)

        fun onPlaybackParametersChanged(mid: String, playbackParameters: PlaybackParameters)

        fun onPositionDiscontinuity(mid: String)

        fun onLoadingChanged(mid: String, isLoading: Boolean)

        fun onTracksChanged(mid: String, trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray)

        fun onTimelineChanged(mid: String, timeline: Timeline, manifest: Any)

        fun onPlayerError(mid: String, error: ExoPlaybackException)
    }

    open class MediaPlayerListenerWrapper : OnMediaPlayerListener {

        override fun onVideoSizeChanged(
            mid: String,
            width: Int,
            height: Int,
            unappliedRotationDegrees: Int,
            pixelWidthHeightRatio: Float
        ) {
        }

        override fun onRenderedFirstFrame(mid: String) {}

        override fun onPlayerStateChanged(mid: String, playWhenReady: Boolean, playbackState: Int) {}

        override fun onPlaybackParametersChanged(mid: String, playbackParameters: PlaybackParameters) {}

        override fun onPositionDiscontinuity(mid: String) {}

        override fun onLoadingChanged(mid: String, isLoading: Boolean) {}

        override fun onTracksChanged(mid: String, trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

        override fun onTimelineChanged(mid: String, timeline: Timeline, manifest: Any) {}

        override fun onPlayerError(mid: String, error: ExoPlaybackException) {}
    }

    companion object {
        @SuppressLint("StaticFieldLeak")

        private fun isBehindLiveWindow(e: ExoPlaybackException): Boolean {
            if (e.type != ExoPlaybackException.TYPE_SOURCE) {
                return false
            }
            var cause: Throwable? = e.sourceException
            while (cause != null) {
                if (cause is BehindLiveWindowException) {
                    return true
                }
                cause = cause.cause
            }
            return false
        }
    }
}
