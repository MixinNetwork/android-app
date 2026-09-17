package one.mixin.android.util.mlkit.scan.analyze

import android.graphics.Rect
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class ScanResultSelectorTest {
    @Test
    fun `single result tracks before stable auto handle`() {
        val selector = ScanResultSelector(stableFrames = 2)
        val item = scanItem("mixin://one")

        assertIs<ScanDecision.Track>(selector.select(listOf(item)))
        val decision = selector.select(listOf(item))

        assertIs<ScanDecision.AutoHandle>(decision)
        assertEquals("mixin://one", decision.text)
    }

    @Test
    fun `different single result resets stability`() {
        val selector = ScanResultSelector(stableFrames = 2)

        selector.select(listOf(scanItem("mixin://one")))
        val decision = selector.select(listOf(scanItem("mixin://two")))

        assertIs<ScanDecision.Track>(decision)
        assertEquals("mixin://two", decision.item.text)
    }

    @Test
    fun `multiple results ask user to choose`() {
        val selector = ScanResultSelector(stableFrames = 2)
        val first = scanItem("mixin://one")
        val second = scanItem("mixin://two")

        val decision = selector.select(listOf(first, second))

        assertIs<ScanDecision.ShowChoices>(decision)
        assertEquals(listOf(first, second), decision.items)
    }

    @Test
    fun `center crop mapper transforms source rect to preview rect`() {
        val rect =
            ScanCoordinateMapper.transform(
                Rect(100, 50, 300, 250),
                sourceWidth = 400,
                sourceHeight = 300,
                destWidth = 800,
                destHeight = 800,
            )

        assertEquals(133.33333f, rect.left, absoluteTolerance = 0.01f)
        assertEquals(133.33333f, rect.top, absoluteTolerance = 0.01f)
        assertEquals(666.6667f, rect.right, absoluteTolerance = 0.01f)
        assertEquals(666.6667f, rect.bottom, absoluteTolerance = 0.01f)
    }

    private fun scanItem(text: String) =
        BarcodeScanItem(
            text = text,
            boundingBox = Rect(10, 20, 110, 120),
            cornerPoints = emptyList(),
            sourceWidth = 200,
            sourceHeight = 200,
        )
}
