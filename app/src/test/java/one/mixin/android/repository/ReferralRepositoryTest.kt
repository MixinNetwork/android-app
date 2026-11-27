package one.mixin.android.repository

import kotlinx.coroutines.runBlocking
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import one.mixin.android.api.response.referral.ReferralResponse
import one.mixin.android.api.service.ReferralService
import one.mixin.android.session.Session
import one.mixin.android.vo.Account
import one.mixin.android.vo.Membership
import one.mixin.android.vo.Plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
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

    @Test
    fun `invited lookup requests referral without membership and preserves unknown on failure`() = runBlocking<Unit> {
        val service = mock(ReferralService::class.java)
        val repository = ReferralRepository(service, mock(UserRepository::class.java))
        val invited = invitedResponse(true)
        val uninvited = invitedResponse(false)
        `when`(service.referral())
            .thenReturn(invited, uninvited, MixinResponse(ResponseError(500, 500, "failed")))
            .thenThrow(IllegalStateException("connection lost"))

        val accountField = Session::class.java.getDeclaredField("self").apply { isAccessible = true }
        val previousAccount = accountField.get(Session)
        try {
            accountField.set(Session, mock(Account::class.java))
            assertNull(requireNotNull(Session.getAccount()).membership)
            assertEquals(true, repository.fetchHasBeenInvitedOrNull("test"))
            assertEquals(false, repository.fetchHasBeenInvitedOrNull("test"))
            assertNull(repository.fetchHasBeenInvitedOrNull("test"))
            assertNull(repository.fetchHasBeenInvitedOrNull("test"))
            verify(service, times(4)).referral()
        } finally {
            accountField.set(Session, previousAccount)
        }
    }

    private fun invitedResponse(invited: Boolean) = MixinResponse<ReferralResponse>().apply {
        val referral = mock(ReferralResponse::class.java)
        `when`(referral.hasBeenInvited).thenReturn(invited)
        data = referral
    }
}
