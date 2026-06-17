package one.mixin.android.ui.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferralShareCardHelperTest {
    @Test
    fun `isZeroPercent returns true for zero percent strings`() {
        assertTrue("0%".isZeroPercent())
        assertTrue(" 0.00 % ".isZeroPercent())
    }

    @Test
    fun `isZeroPercent returns false for non zero percent strings`() {
        assertFalse("20%".isZeroPercent())
        assertFalse("".isZeroPercent())
    }
}
