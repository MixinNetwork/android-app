package one.mixin.android.api.referral

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PendingReferralStorePostSignUpTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val signUpAt = 1_000_000L
    private val minute = 60L * 1000

    @Before
    fun setUp() {
        PendingReferralStore.clear(context)
    }

    @Test
    fun getForPostSignUp_requiresSignUpMarker() {
        PendingReferralStore.save(context, "crmlsv", signUpAt)
        assertNull(PendingReferralStore.getForPostSignUp(context, signUpAt))
    }

    @Test
    fun getForPostSignUp_returnsCodeOnFirstHomeSessionAfterSignUp() {
        PendingReferralStore.save(context, "crmlsv", signUpAt)
        PendingReferralStore.markSignUpCompleted(context)
        assertEquals("CRMLSV", PendingReferralStore.getForPostSignUp(context, signUpAt + 2 * 24 * 60 * minute))
    }

    @Test
    fun getForPostSignUp_returnsLateCodeInSameSession() {
        PendingReferralStore.markSignUpCompleted(context)
        assertNull(PendingReferralStore.getForPostSignUp(context, signUpAt))
        PendingReferralStore.save(context, "crmlsv", signUpAt + minute)
        assertEquals("CRMLSV", PendingReferralStore.getForPostSignUp(context, signUpAt + minute))
    }

    @Test
    fun getForPostSignUp_doesNotReturnCodeInLaterSession() {
        PendingReferralStore.save(context, "crmlsv", signUpAt)
        PendingReferralStore.markSignUpCompleted(context)
        assertEquals("CRMLSV", PendingReferralStore.getForPostSignUp(context, signUpAt))
        PendingReferralStore.endPostSignUpSessionForTest()
        PendingReferralStore.save(context, "other1", signUpAt + minute)
        assertNull(PendingReferralStore.getForPostSignUp(context, signUpAt + minute))
    }

    @Test
    fun getForPostSignUp_returnsNullWhenPendingCodeExpired() {
        val expiredAt = signUpAt + 7L * 24 * 60 * minute + minute
        PendingReferralStore.save(context, "crmlsv", signUpAt)
        PendingReferralStore.markSignUpCompleted(context)
        assertNull(PendingReferralStore.getForPostSignUp(context, expiredAt))
    }

    @Test
    fun clear_removesPendingCodeAndSignUpMarker() {
        PendingReferralStore.save(context, "crmlsv", signUpAt)
        PendingReferralStore.markSignUpCompleted(context)
        PendingReferralStore.clear(context)
        assertNull(PendingReferralStore.get(context, signUpAt))
        assertNull(PendingReferralStore.getForPostSignUp(context, signUpAt))
    }
}
