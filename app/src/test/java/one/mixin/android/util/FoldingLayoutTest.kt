package one.mixin.android.util

import android.app.Application
import android.graphics.Rect
import androidx.window.embedding.SplitAttributes
import androidx.window.layout.FoldingFeature
import one.mixin.android.ui.conversation.ConversationEmbedding
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [35])
class FoldingLayoutTest {
    private class Fold(
        override val bounds: Rect,
        override val orientation: FoldingFeature.Orientation = FoldingFeature.Orientation.HORIZONTAL,
        override val state: FoldingFeature.State = FoldingFeature.State.HALF_OPENED,
        override val isSeparating: Boolean = true,
        override val occlusionType: FoldingFeature.OcclusionType = FoldingFeature.OcclusionType.NONE,
    ) : FoldingFeature

    @Test
    fun horizontalTabletopKeepsThePageAboveTheCrease() {
        val container = Rect(0, 0, 1000, 2000)
        val fold = Fold(Rect(0, 1100, 1000, 1140))
        assertEquals(Rect(0, 0, 0, 900), separatingFoldPadding(fold, container))
        assertEquals(
            Rect(),
            separatingFoldPadding(
                Fold(Rect(0, 1100, 1000, 1140), state = FoldingFeature.State.FLAT, isSeparating = false),
                container,
            ),
        )
    }

    @Test
    fun bottomSheetUsesTheLowerPaneOnASmallFold() {
        val container = Rect(0, 0, 1080, 2400)
        val fold = Fold(Rect(0, 1100, 1080, 1160))
        val frame = bottomSheetFoldFrame(fold, container, 160)
        assertEquals(1240, frame.maxHeight)
        assertEquals(0, frame.bottom)
        assertEquals(0, frame.left)
    }

    @Test
    fun bottomSheetMovesAboveACreaseThatLeavesNoRoomBelow() {
        val container = Rect(0, 0, 1080, 2000)
        val fold = Fold(Rect(0, 1800, 1080, 1900))
        val frame = bottomSheetFoldFrame(fold, container, 160)
        assertEquals(1800, frame.maxHeight)
        assertEquals(200, frame.bottom)
    }

    @Test
    fun bottomSheetStaysBesideAVerticalHinge() {
        val container = Rect(0, 0, 2000, 1000)
        val frame = bottomSheetFoldFrame(Fold(Rect(1400, 0, 1480, 1000), FoldingFeature.Orientation.VERTICAL), container, 160)
        assertEquals(600, frame.right)
        assertEquals(1000, frame.maxHeight)
        assertEquals(0, frame.bottom)
    }

    @Test
    fun singlePaneContentAvoidsTheHingeAndRestoresWithoutIt() {
        val container = Rect(0, 0, 1000, 800)
        assertEquals(Rect(380, 0, 0, 0), separatingFoldPadding(Fold(Rect(360, 0, 380, 800), FoldingFeature.Orientation.VERTICAL), container))
        assertEquals(Rect(0, 0, 400, 0), separatingFoldPadding(Fold(Rect(600, 0, 620, 800), FoldingFeature.Orientation.VERTICAL), container))
        assertEquals(Rect(0, 0, 0, 400), separatingFoldPadding(Fold(Rect(0, 400, 1000, 420)), container))
        assertEquals(Rect(), separatingFoldPadding(null, container))
        assertEquals(Rect(), separatingFoldPadding(Fold(Rect(1200, 0, 1220, 800), FoldingFeature.Orientation.VERTICAL), container))
    }

    @Test
    fun tabletopUsesLocalCoordinatesAndAcceptsAZeroWidthCrease() {
        val container = Rect(100, 100, 700, 900)
        assertEquals(Rect(0, 300, 600, 320), tabletopFoldBounds(Fold(Rect(0, 400, 1000, 420)), container, 160))
        assertEquals(Rect(0, 300, 600, 300), tabletopFoldBounds(Fold(Rect(0, 400, 1000, 400)), container, 160))
    }

    @Test
    fun flatOutsideAndCrampedWindowsKeepTheNormalLayout() {
        val container = Rect(0, 0, 600, 800)
        assertNull(tabletopFoldBounds(Fold(Rect(0, 400, 600, 400), state = FoldingFeature.State.FLAT, isSeparating = false), container, 160))
        assertNull(tabletopFoldBounds(Fold(Rect(0, 100, 600, 100)), container, 160))
        assertNull(tabletopFoldBounds(Fold(Rect(0, 900, 600, 900)), container, 160))
        assertNull(tabletopFoldBounds(Fold(Rect(0, 400, 300, 400)), container, 160))
        assertNull(tabletopFoldBounds(null, container, 160))
    }

    @Test
    fun splitUsesTheHingeOnlyWhenBothPanesFitIncludingRtl() {
        val defaults = SplitAttributes.Builder().setSplitType(SplitAttributes.SplitType.ratio(0.4f)).build()
        val bounds = Rect(0, 0, 1000, 800)
        val hinge = Fold(Rect(360, 0, 380, 800), FoldingFeature.Orientation.VERTICAL)
        fun split(fold: FoldingFeature?, rtl: Boolean = false, fits: Boolean = true) =
            ConversationEmbedding.splitAttributes(fits, bounds, 1f, fold, rtl, defaults).splitType
        assertEquals(SplitAttributes.SplitType.SPLIT_TYPE_HINGE, split(hinge))
        assertEquals(SplitAttributes.SplitType.SPLIT_TYPE_EXPAND, split(hinge, rtl = true))
        assertEquals(SplitAttributes.SplitType.SPLIT_TYPE_EXPAND, split(null, fits = false))
        assertEquals(defaults.splitType, split(null))
        assertEquals(SplitAttributes.SplitType.SPLIT_TYPE_EXPAND, split(Fold(Rect(0, 400, 1000, 400))))
    }
}
