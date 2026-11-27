package one.mixin.android.extension

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DetectImageTypeTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun detectsPng() {
        assertEquals("image/png" to "png", detectImageType(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D)))
    }

    @Test
    fun detectsJpeg() {
        assertEquals("image/jpeg" to "jpg", detectImageType(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46)))
    }

    @Test
    fun detectsGif() {
        assertEquals("image/gif" to "gif", detectImageType("GIF89a".toByteArray()))
        assertEquals("image/gif" to "gif", detectImageType("GIF87a".toByteArray()))
    }

    @Test
    fun detectsWebp() {
        val header = "RIFF".toByteArray() + bytes(0x24, 0x00, 0x00, 0x00) + "WEBP".toByteArray()
        assertEquals("image/webp" to "webp", detectImageType(header))
    }

    @Test
    fun returnsNullForUnknownOrShortHeader() {
        assertNull(detectImageType("<svg xmlns=".toByteArray()))
        assertNull(detectImageType("RIFF".toByteArray() + bytes(0x24, 0x00, 0x00, 0x00) + "WAVE".toByteArray()))
        assertNull(detectImageType(bytes(0x89, 0x50)))
        assertNull(detectImageType(ByteArray(0)))
    }
}
