package one.mixin.android.vo

import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.Instant

class MembershipTest {
    @Test
    fun `active plan returns plan when membership is active`() {
        val membership = Membership(
            plan = Plan.ELITE,
            expiredAt = Instant.now().plusSeconds(60).toString(),
        )

        assertEquals(Plan.ELITE, membership.activePlan())
    }

    @Test
    fun `active plan returns none when membership is expired`() {
        val membership = Membership(
            plan = Plan.ELITE,
            expiredAt = Instant.now().minusSeconds(60).toString(),
        )

        assertEquals(Plan.None, membership.activePlan())
    }

    @Test
    fun `active plan returns none when membership is null`() {
        val membership: Membership? = null

        assertEquals(Plan.None, membership.activePlan())
    }
}
