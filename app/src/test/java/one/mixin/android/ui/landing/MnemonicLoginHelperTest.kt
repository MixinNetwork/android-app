package one.mixin.android.ui.landing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MnemonicLoginHelperTest {
    @Test
    fun `completes 12 word phrase before login processing`() {
        val words = (1..12).map { "word$it" }

        val result = completeMnemonicForLogin(words) { input ->
            input + "checksum"
        }

        assertEquals(words + "checksum", result)
    }

    @Test
    fun `completes 24 word phrase before login processing`() {
        val words = (1..24).map { "word$it" }

        val result = completeMnemonicForLogin(words) { input ->
            input + "checksum"
        }

        assertEquals(words + "checksum", result)
    }

    @Test
    fun `keeps 13 and 25 word phrases unchanged`() {
        val thirteenWords = (1..13).map { "word$it" }
        val twentyFiveWords = (1..25).map { "word$it" }

        assertEquals(thirteenWords, completeMnemonicForLogin(thirteenWords) { error("unused") })
        assertEquals(twentyFiveWords, completeMnemonicForLogin(twentyFiveWords) { error("unused") })
    }

    @Test
    fun `rejects unsupported word counts`() {
        assertFailsWith<IllegalArgumentException> {
            completeMnemonicForLogin((1..11).map { "word$it" }) { error("unused") }
        }
    }
}
