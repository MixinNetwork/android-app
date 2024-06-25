package one.mixin.android.mock

import one.mixin.android.vo.Account

const val MOCK_ME_USER_ID = "3B035776-963D-49ED-850C-22EC30965481"

fun mockAccount(): Account {
    return Account(
        MOCK_ME_USER_ID,
        "94EF6A7F-52A6-4019-996F-C30C77F248A6",
        "user",
        "7000",
        "ME",
        "Mixin",
        "Mixin",
        "",
        "",
        "",
        "", // PinToken is AES key encrypted with RSA
        "",
        "",
        "2018-10-01T02:17:44.806365421Z",
        "",
        true,
        "",
        0,
        "EVERYBODY",
        "EVERYBODY",
        true,
        true,
        "",
        0.0,
        0.0,
        arrayListOf(),
        "",
    )
}
