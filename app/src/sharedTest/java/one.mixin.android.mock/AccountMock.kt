package one.mixin.android.mock

import one.mixin.android.vo.Account

const val MOCK_ME_USER_ID = "3B035776-963D-49ED-850C-22EC30965481"

fun mockAccount(): Account {
    return Account(
        userId = MOCK_ME_USER_ID,
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
        pinToken = "", // PinToken is AES key encrypted with RSA
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
