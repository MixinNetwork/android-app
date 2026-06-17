package one.mixin.android.api.referral

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReferralShareHelperTest {
    @Test
    fun `calculate rebate percent from trading and inviter ratios`() {
        assertEquals("20%", calculateReferralRebatePercentOrNull("0.25", "0.2"))
    }

    @Test
    fun `calculate rebate percent keeps zero values`() {
        assertEquals("0%", calculateReferralRebatePercentOrNull("0.25", "1"))
    }

    @Test
    fun `calculate rebate percent returns null when trading ratio is invalid`() {
        assertNull(calculateReferralRebatePercentOrNull("oops", "0.2"))
    }

    @Test
    fun `calculate rebate percent returns null when inviter ratio is invalid`() {
        assertNull(calculateReferralRebatePercentOrNull("0.25", "oops"))
    }
}
