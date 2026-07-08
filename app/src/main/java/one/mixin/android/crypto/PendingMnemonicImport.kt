package one.mixin.android.crypto

import android.content.Context
import one.mixin.android.Constants

fun savePendingImportMnemonic(context: Context, words: List<String>) {
    storeValueInEncryptedPreferences(context, Constants.Tip.PENDING_IMPORT_MNEMONIC, toEntropy(words))
}

fun getPendingImportMnemonic(context: Context): String? {
    val entropy = getValueFromEncryptedPreferences(context, Constants.Tip.PENDING_IMPORT_MNEMONIC) ?: return null
    return runCatching { toMnemonic(entropy) }.getOrNull()
}

fun hasPendingImportMnemonic(context: Context): Boolean {
    return getValueFromEncryptedPreferences(context, Constants.Tip.PENDING_IMPORT_MNEMONIC) != null
}

fun clearPendingImportMnemonic(context: Context) {
    removeValueFromEncryptedPreferences(context, Constants.Tip.PENDING_IMPORT_MNEMONIC)
}
