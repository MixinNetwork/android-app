package one.mixin.android.util.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTranscodeProfileFactoryTest {
    @Test
    fun `resize 4k landscape video to 720p profile`() {
        val profile = VideoTranscodeProfileFactory.create(3840, 2160, 20_000_000)

        assertEquals(1280, profile.width)
        assertEquals(720, profile.height)
        assertEquals(2_600_000, profile.bitrate)
        assertTrue(profile.needsResize(3840, 2160))
    }

    @Test
    fun `preserve proportionally lower source bitrate`() {
        val profile = VideoTranscodeProfileFactory.create(1920, 1080, 2_000_000)

        assertEquals(1_333_333, profile.bitrate)
    }

    @Test
    fun `apply telegram bitrate caps across output dimensions`() {
        val squareProfile = VideoTranscodeProfileFactory.create(3840, 3840, 30_000_000)
        val wideProfile = VideoTranscodeProfileFactory.create(3840, 1440, 20_000_000)
        val ultraWideProfile = VideoTranscodeProfileFactory.create(3840, 960, 20_000_000)

        assertEquals(6_800_000, squareProfile.bitrate)
        assertEquals(1_000_000, wideProfile.bitrate)
        assertEquals(750_000, ultraWideProfile.bitrate)
    }

    @Test
    fun `preserve source profile when resize is unnecessary`() {
        val profile = VideoTranscodeProfileFactory.create(1280, 720, 4_000_000)

        assertEquals(1280, profile.width)
        assertEquals(720, profile.height)
        assertEquals(4_000_000, profile.bitrate)
        assertFalse(profile.needsResize(1280, 720))
    }

    @Test
    fun `use telegram fallback when source bitrate is unavailable`() {
        val profile = VideoTranscodeProfileFactory.create(640, 360, 0)

        assertEquals(921_600, profile.bitrate)
    }
}
