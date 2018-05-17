package one.mixin.android.util.video

import android.net.Uri
import android.view.TextureView
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import one.mixin.android.MixinApplication
import java.util.concurrent.TimeUnit

class MixinPlayer : Player.EventListener, VideoListener {

    val player: SimpleExoPlayer by lazy {
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(BANDWIDTH_METER)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        ExoPlayerFactory.newSimpleInstance(MixinApplication.appContext, trackSelector).apply {
            addListener(this@MixinPlayer)
            addVideoListener(this@MixinPlayer)
        }
    }
    private var onVideoPlayerListener: OnVideoPlayerListener? = null
    private var mHlsMediaSource: MediaSource? = null

    fun isPlaying() = player.playWhenReady

    fun duration() = if (player.duration == C.TIME_UNSET) 0 else player.duration.toInt()

    fun videoFormat() = player.videoFormat

    fun currentPosition() = player.currentPosition

    val period = Timeline.Period()

    fun getCurrentPos(): Long {
        var position = player.currentPosition
        val currentTimeline = player.currentTimeline
        if (!currentTimeline.isEmpty) {
            position -= currentTimeline.getPeriod(player.currentPeriodIndex, period)
                .positionInWindowMs
        }
        return position
    }

    fun release() {
        player.release()
    }

    fun stop() = player.stop()

    fun start() {
        player.playWhenReady = true
    }

    fun pause() {
        player.playWhenReady = false
    }

    fun seekTo(pos: Long) {
        player.seekTo(pos)
    }

    fun seekTo(timeMillis: Int) {
        val seekPos = (if (player.duration == C.TIME_UNSET)
            0
        else
            Math.min(Math.max(0, timeMillis), duration())).toLong()
        seekTo(seekPos)
    }

    fun setVolume(volume: Float) {
        player.volume = volume
    }

    fun setVideoTextureView(texture: TextureView) {
        player.setVideoTextureView(texture)
    }

    fun loadVideo(url: String) {
        val mediaSource = ExtractorMediaSource.Factory(buildDataSourceFactory(BANDWIDTH_METER))
            .createMediaSource(Uri.parse(url))
        player.prepare(mediaSource)
    }
    private fun buildDataSourceFactory(bandwidthMeter: DefaultBandwidthMeter): DataSource.Factory {
        return DefaultDataSourceFactory(MixinApplication.appContext, bandwidthMeter, buildOkHttpDataSourceFactory())
    }

    private fun buildOkHttpDataSourceFactory(): OkHttpDataSourceFactory {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.NONE
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor(logging)
            .build()
        return OkHttpDataSourceFactory(okHttpClient, Util.getUserAgent(MixinApplication.appContext, "Shou"), null)
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
        onVideoPlayerListener?.onTracksChanged(trackGroups, trackSelections)
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        onVideoPlayerListener?.onLoadingChanged(isLoading)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        onVideoPlayerListener?.onPlayerStateChanged(playWhenReady, playbackState)

        if (playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        if (isBehindLiveWindow(error)) {
            player.prepare(mHlsMediaSource)
        }
        // HttpDataSourceException
    }

    override fun onPositionDiscontinuity(reason: Int) {
        onVideoPlayerListener?.onPositionDiscontinuity()
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        onVideoPlayerListener?.onPlaybackParametersChanged(playbackParameters)
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
    }

    override fun onRenderedFirstFrame() {
        onVideoPlayerListener?.onRenderedFirstFrame()
    }

    fun setOnVideoPlayerListener(onVideoPlayerListener: OnVideoPlayerListener) {
        this.onVideoPlayerListener = onVideoPlayerListener
    }

    fun setSpeed(speed: Float) {
        val pp = PlaybackParameters(speed, player.playbackParameters.pitch)
        player.playbackParameters = pp
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
    }

    open class VideoPlayerListenerWrapper : OnVideoPlayerListener {
        override fun onVideoSizeChanged(
            width: Int,
            height: Int,
            unappliedRotationDegrees: Int,
            pixelWidthHeightRatio: Float
        ) {}

        override fun onRenderedFirstFrame() {}

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}

        override fun onPositionDiscontinuity() {}

        override fun onLoadingChanged(isLoading: Boolean) {}

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

        override fun onTimelineChanged(timeline: Timeline, manifest: Any) {}
    }

    companion object {
        private val BANDWIDTH_METER = DefaultBandwidthMeter()

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