package one.mixin.android.session

import io.jsonwebtoken.EdDSAPrivateKey
import okhttp3.Request
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.vo.Account
import one.mixin.eddsa.KeyPair
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSignTokenTest {
    @Test
    fun legacyTokenExpiresFiveMinutesAfterIssue() {
        val key = EdDSAPrivateKey(KeyPair.newKeyPair(true).privateKey)
        val request = Request.Builder().url("https://api.mixin.one/me").build()
        val token = Session.signLegacyToken(mockAccount(), request, "request-id", key)
        val claims = JSONObject(String(token.split(".")[1].base64RawURLDecode()))
        assertEquals(300L, claims.getLong("exp") - claims.getLong("iat"))
    }

    private fun mockAccount(): Account =
        Account(
            userId = "3B035776-963D-49ED-850C-22EC30965481",
            sessionId = "94EF6A7F-52A6-4019-996F-C30C77F248A6",
            type = "user",
            identityNumber = "7000",
            relationship = "ME",
            fullName = "Mixin",
            biography = "Mixin",
            avatarUrl = "",
            phone = "",
            phoneVerifiedAt = "",
            avatarBase64 = "",
            pinToken = "",
            codeId = "",
            codeUrl = "",
            createdAt = "2018-10-01T02:17:44.806365421Z",
            receiveMessageSource = "",
            hasPin = true,
            tipKeyBase64 = "",
            tipCounter = 0,
            acceptConversationSource = "EVERYBODY",
            acceptSearchSource = "EVERYBODY",
            hasEmergencyContact = true,
            hasSafe = true,
            fiatCurrency = "",
            transferNotificationThreshold = 0.0,
            transferConfirmationThreshold = 0.0,
            features = arrayListOf(),
            salt = "",
            membership = null,
            system = null,
            saltExportedAt = null,
            level = null,
        )
}
