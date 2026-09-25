package one.mixin.android.ui.home.reminder

import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals

class BatteryOptimizationContentTest {
    @Test
    fun markdownBoldMarkersBecomeSpanStyles() {
        val content = batteryOptimizationAnnotatedContent("Select **Optimised** or **Unrestricted**.")

        assertEquals("Select Optimised or Unrestricted.", content.text)
        val boldRanges = content.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(2, boldRanges.size)
        assertEquals("Optimised", content.text.substring(boldRanges[0].start, boldRanges[0].end))
        assertEquals("Unrestricted", content.text.substring(boldRanges[1].start, boldRanges[1].end))
    }

    @Test
    fun htmlBoldMarkersBecomeSpanStyles() {
        val content = batteryOptimizationAnnotatedContent("Turn <b>Allow background activity</b> on.")

        assertEquals("Turn Allow background activity on.", content.text)
        val boldRanges = content.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, boldRanges.size)
        assertEquals("Allow background activity", content.text.substring(boldRanges[0].start, boldRanges[0].end))
    }
}
