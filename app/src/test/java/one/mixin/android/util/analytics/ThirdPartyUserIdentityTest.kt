package one.mixin.android.util.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

class ThirdPartyUserIdentityTest {
    @Test
    fun hashedUserIdMatchesKnownUuidVectors() {
        listOf(
            "550e8400-e29b-41d4-a716-446655440000" to "a3a9e1ed9732cab28868127be00f1ce921acaefdd5c3b23a6e9e0072bd9c1a34",
            "6ba7b810-9dad-11d1-80b4-00c04fd430c8" to "e5855ff48799c52c9ccf80b82bab9492c347a316876dbeaafef22b0bd4fac13d",
            "123e4567-e89b-12d3-a456-426614174000" to "986c0dc956dc822b5d8f698661b9eb1ef880786ff9043c16744d2a420e99e9bb",
        ).forEach { (userId, expectedHash) ->
            assertEquals(expectedHash, ThirdPartyUserIdentity.hashedUserId(userId), userId)
        }
    }
}
