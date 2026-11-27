package one.mixin.android.ui.tip

import one.mixin.android.vo.Account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TipFlowInteractorReferralTest {
    @Test
    fun shouldMarkPostSignUpReferral_onlyForCreateOpeningMainActivity() {
        assertTrue(shouldMarkPostSignUpReferral(TipType.Create, shouldOpenMainActivity = true))
        assertFalse(shouldMarkPostSignUpReferral(TipType.Create, shouldOpenMainActivity = false))
        assertFalse(shouldMarkPostSignUpReferral(TipType.Upgrade, shouldOpenMainActivity = true))
        assertFalse(shouldMarkPostSignUpReferral(TipType.Change, shouldOpenMainActivity = true))
    }

    @Test
    fun persistSafeRegisteredAccount_marksSignUpBeforeStoringSafeAccount() {
        val events = mutableListOf<String>()
        val account = testAccount(hasSafe = true)

        persistSafeRegisteredAccount(
            account = account,
            shouldMarkPostSignUpReferral = true,
            markSignUpCompleted = { events.add("markSignUpCompleted") },
            storeAccount = { stored -> events.add("storeAccount:${stored.userId}") },
        )

        assertEquals(listOf("markSignUpCompleted", "storeAccount:${account.userId}"), events)
    }

    @Test
    fun persistSafeRegisteredAccount_doesNotMarkSignUpWhenUnsafeOrDisabled() {
        val unsafeEvents = mutableListOf<String>()
        persistSafeRegisteredAccount(
            account = testAccount(hasSafe = false),
            shouldMarkPostSignUpReferral = true,
            markSignUpCompleted = { unsafeEvents.add("markSignUpCompleted") },
            storeAccount = { unsafeEvents.add("storeAccount") },
        )
        assertEquals(listOf("storeAccount"), unsafeEvents)

        val nonSignUpEvents = mutableListOf<String>()
        persistSafeRegisteredAccount(
            account = testAccount(hasSafe = true),
            shouldMarkPostSignUpReferral = false,
            markSignUpCompleted = { nonSignUpEvents.add("markSignUpCompleted") },
            storeAccount = { nonSignUpEvents.add("storeAccount") },
        )
        assertEquals(listOf("storeAccount"), nonSignUpEvents)
    }

    private fun testAccount(hasSafe: Boolean): Account = Account(
        userId = "user-1",
        sessionId = "session-1",
        type = "user",
        identityNumber = "10001",
        relationship = "ME",
        fullName = "Test User",
        biography = "",
        avatarUrl = null,
        phone = "+10000000000",
        phoneVerifiedAt = null,
        avatarBase64 = null,
        pinToken = "pin-token",
        codeId = "code-id",
        codeUrl = "https://mixin.one/codes/1",
        createdAt = "2026-11-26T00:00:00Z",
        receiveMessageSource = "EVERYBODY",
        hasPin = true,
        tipKeyBase64 = "tip-key",
        tipCounter = 1,
        acceptConversationSource = "EVERYBODY",
        acceptSearchSource = "EVERYBODY",
        hasEmergencyContact = false,
        hasSafe = hasSafe,
        fiatCurrency = "USD",
        transferNotificationThreshold = 0.0,
        transferConfirmationThreshold = 0.0,
        salt = null,
        membership = null,
        system = null,
        saltExportedAt = null,
        level = null,
    )
}
