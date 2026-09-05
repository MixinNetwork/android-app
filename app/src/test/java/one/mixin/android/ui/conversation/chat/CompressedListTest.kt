package one.mixin.android.ui.conversation.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class CompressedListTest {
    @Test
    fun windowRemainsBoundedWhenScrollingBothWays() {
        val data = CompressedList((0 until 600).toList())
        repeat(1000) { page ->
            data.append((600 + page * 30 until 630 + page * 30).toList())
            assertEquals(30, data.trim(600, fromStart = true))
            assertEquals(600, data.size)
        }
        data.prepend((-30 until 0).toList())
        assertEquals(30, data.trim(600, fromStart = false))
        assertEquals(-30, data.first())
        data.update(0, -31)
        assertEquals(-31, data.first())
        data.removeAll { it != null && it < 0 }
        assertEquals(570, data.size)
    }
}
