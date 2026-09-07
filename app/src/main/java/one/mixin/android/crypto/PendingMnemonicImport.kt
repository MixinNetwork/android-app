package one.mixin.android.crypto

import android.content.Context
import one.mixin.android.Constants
import one.mixin.android.extension.defaultSharedPreferences

private const val LEGACY_PENDING_IMPORT_MNEMONIC = "pending_import_mnemonic"
private val pendingImportMnemonicLock = Any()
private var pendingImportMnemonicEntropy: ByteArray? = null

fun preparePendingImportMnemonic(words: List<String>) {
    synchronized(pendingImportMnemonicLock) {
        pendingImportMnemonicEntropy?.fill(0)
        pendingImportMnemonicEntropy = toEntropy(words)
    }
}

fun getPendingImportMnemonicEntropy(): ByteArray? =
    synchronized(pendingImportMnemonicLock) {
        pendingImportMnemonicEntropy?.copyOf()
    }

fun hasPendingImportMnemonicInMemory(): Boolean =
    synchronized(pendingImportMnemonicLock) {
        pendingImportMnemonicEntropy != null
    }

fun replacePendingImportMnemonicEntropy(entropy: ByteArray) {
    synchronized(pendingImportMnemonicLock) {
        if (pendingImportMnemonicEntropy == null) return
        pendingImportMnemonicEntropy?.fill(0)
        pendingImportMnemonicEntropy = entropy.copyOf()
    }
}

fun markPendingImportMnemonic(context: Context) {
    removeValueFromEncryptedPreferences(context, LEGACY_PENDING_IMPORT_MNEMONIC)
    context.defaultSharedPreferences.edit()
        .putBoolean(Constants.Account.PREF_PENDING_MNEMONIC_IMPORT, true)
        .commit()
}

fun hasPendingImportMnemonic(context: Context): Boolean {
    return context.defaultSharedPreferences.getBoolean(Constants.Account.PREF_PENDING_MNEMONIC_IMPORT, false)
}

fun clearPendingImportMnemonic(context: Context) {
    synchronized(pendingImportMnemonicLock) {
        pendingImportMnemonicEntropy?.fill(0)
        pendingImportMnemonicEntropy = null
    }
    removeValueFromEncryptedPreferences(context, LEGACY_PENDING_IMPORT_MNEMONIC)
    context.defaultSharedPreferences.edit()
        .remove(Constants.Account.PREF_PENDING_MNEMONIC_IMPORT)
        .commit()
}
