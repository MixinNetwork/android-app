package one.mixin.android.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareImageSizeTest {
    @Test
    fun `normalize sizes while preserving aspect ratio`() {
        assertEquals(1024 to 1024, shareImageSize(300, 300))
        assertEquals(1024 to 1024, shareImageSize(1500, 1500))
        assertEquals(768 to 1024, shareImageSize(300, 400))
        assertEquals(512 to 1024, shareImageSize(300, 600))
        assertEquals(959 to 1024, shareImageSize(337, 360))
        assertEquals(1024 to 512, shareImageSize(2000, 1000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject unmeasured views`() {
        shareImageSize(0, 300)
    }
}
