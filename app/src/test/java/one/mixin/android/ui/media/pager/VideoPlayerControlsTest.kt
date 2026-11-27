package one.mixin.android.ui.media.pager

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import one.mixin.android.util.video.VideoPlayerControls
import org.junit.Test

class VideoPlayerControlsTest {
    @Test
    fun cyclesSpeedUsingFloatTolerance() {
        assertEquals(1.5f, VideoPlayerControls.nextSpeed(1.249f), 0.001f)
        assertEquals(1.5f, VideoPlayerControls.nextSpeed(1.25f), 0.001f)
        assertEquals(2f, VideoPlayerControls.nextSpeed(1.5f), 0.001f)
        assertEquals(1.25f, VideoPlayerControls.nextSpeed(1f), 0.001f)
        assertEquals(1.25f, VideoPlayerControls.nextSpeed(1.1f), 0.001f)
        assertEquals(1f, VideoPlayerControls.nextSpeed(2f), 0.001f)
    }

    @Test
    fun clampsSeekTargetForKnownAndUnknownDuration() {
        assertEquals(0L, VideoPlayerControls.seekTarget(500L, -1_000L, 10_000L))
        assertEquals(10_000L, VideoPlayerControls.seekTarget(9_500L, 1_000L, 10_000L))
        assertEquals(0L, VideoPlayerControls.seekTarget(500L, -1_000L, C.TIME_UNSET))
        assertEquals(Long.MAX_VALUE, VideoPlayerControls.seekTarget(1L, Long.MAX_VALUE, C.TIME_UNSET))
        assertEquals(3_000L, VideoPlayerControls.seekTarget(1_000L, 2_000L, C.TIME_UNSET))
    }

    @Test
    fun quantizesFramePreviewPositionsToHalfSecondBuckets() {
        assertEquals(0L, VideoPlayerControls.frameBucket(499L))
        assertEquals(500L, VideoPlayerControls.frameBucket(500L))
        assertEquals(1_000L, VideoPlayerControls.frameBucket(1_499L))
    }
}
