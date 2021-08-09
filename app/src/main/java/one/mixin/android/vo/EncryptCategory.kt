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