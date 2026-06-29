package one.mixin.android.util.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

class ThirdPartyUserIdentityTest {
    @Test
    fun hashedUserIdReturnsSha256Hex() {
        assertEquals(
            "327552a5675d4d1a1f5ab75f3dfe38530dafea8fc9fdb40d28b0f750f4d2d142",
            ThirdPartyUserIdentity.hashedUserId("mixin-user-id"),
        )
    }
}
