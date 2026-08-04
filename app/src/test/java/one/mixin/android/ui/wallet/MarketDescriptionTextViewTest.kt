package one.mixin.android.ui.wallet

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarketDescriptionTextViewTest {
    @Test
    fun collapsedWordBoundaryDoesNotTreatCjkCharactersAsWords() {
        assertTrue(shouldKeepCollapsedWordBoundary('E', 't'))
        assertTrue(shouldKeepCollapsedWordBoundary('1', '2'))

        assertFalse(shouldKeepCollapsedWordBoundary('\u4E2D', '\u6587'))
        assertFalse(shouldKeepCollapsedWordBoundary('\u3042', '\u3044'))
        assertFalse(shouldKeepCollapsedWordBoundary('\uD55C', '\uAE00'))
    }
}
