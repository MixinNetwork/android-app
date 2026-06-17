package one.mixin.android.repository

import one.mixin.android.vo.Membership
import one.mixin.android.vo.Plan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.Instant

class ReferralRepositoryTest {
    @Test
    fun `has valid referral membership returns false when membership is null`() {
        assertFalse(hasValidReferralMembership(null))
    }

    @Test
    fun `has valid referral membership returns false when membership is expired`() {
        val membership = Membership(
            plan = Plan.ADVANCE,
            expiredAt = Instant.now().minusSeconds(60).toString(),
        )

        assertFalse(hasValidReferralMembership(membership))
    }

    @Test
    fun `has valid referral membership returns true when membership is active`() {
        val membership = Membership(
            plan = Plan.ADVANCE,
            expiredAt = Instant.now().plusSeconds(60).toString(),
        )

        assertTrue(hasValidReferralMembership(membership))
    }
}
