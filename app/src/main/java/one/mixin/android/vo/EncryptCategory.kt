package one.mixin.android.vo

enum class EncryptCategory {
    PLAIN,
    SIGNAL,
    ENCRYPTED
}

fun EncryptCategory.toCategory(
    plainCategory: MessageCategory,
    signalCategory: MessageCategory,
    encryptCategory: MessageCategory
): String =
    when (this) {
        EncryptCategory.SIGNAL -> signalCategory
        EncryptCategory.ENCRYPTED -> encryptCategory
        else -> plainCategory
    }.name

fun EncryptCategory.isEncrypt() = this != EncryptCategory.PLAIN

fun EncryptCategory.isPlain() = this == EncryptCategory.PLAIN

fun getEncryptedCategory(isBot: Boolean, app: App?): EncryptCategory {
    return if (isBot && app?.capabilities?.contains(AppCap.ENCRYPTED.name) == true) {
        EncryptCategory.ENCRYPTED
    } else if (isBot) {
        EncryptCategory.PLAIN
    } else {
        EncryptCategory.SIGNAL
    }
}
