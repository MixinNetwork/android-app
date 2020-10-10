package one.mixin.android.mock

import one.mixin.android.vo.Account
import java.util.UUID

fun mockAccount(): Account {
    return Account(
        UUID.randomUUID().toString(), UUID.randomUUID().toString(),
        "user", "7000", "ME", "Mixin", "Mixin",
        "", "", "", "", // PinToken is AES key encrypted with RSA
        "", "", "2018-10-01T02:17:44.806365421Z", "", true, "EVERYBODY",
        "EVERYBODY", true, "USD"
    )
}
