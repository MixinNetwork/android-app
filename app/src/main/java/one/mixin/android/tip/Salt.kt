package one.mixin.android.tip

import android.content.Context
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.isNullOrEmpty
import one.mixin.android.extension.remove
import one.mixin.android.extension.toHex

// legacy: hex
// new   : hex, hex ...
internal fun readEncryptedSalt(context: Context): ByteArray? {
    val salts = context.defaultSharedPreferences.getString(Constants.Tip.SPEND_SALT, null) ?: return null
    val historicalSalts = salts.split(",")
    val salt = historicalSalts.firstOrNull()?.hexStringToByteArray()
    if (salt.isNullOrEmpty()) return null
    return decryptSalt(salt)
}

internal fun storeEncryptedSalt(
    context: Context,
    encryptedSalt: ByteArray,
): Boolean {
    val historicalSaltsStr = context.defaultSharedPreferences.getString(Constants.Tip.SPEND_SALT, null)
    val historicalSalts = if (!historicalSaltsStr.isNullOrBlank()) {
        val list = historicalSaltsStr.split(",").toMutableList()
        val firstSalt = list.firstOrNull()?.hexStringToByteArray()
        if (firstSalt != null && firstSalt.isNotEmpty()) {
            val firstEncryptedSalt = decryptSalt(firstSalt)
            if (firstEncryptedSalt.contentEquals(encryptedSalt)) {
                return true
            }
        }
        list
    } else mutableListOf()

    val cipher = getEncryptCipher(Constants.Tip.ALIAS_SPEND_SALT)
    val edit = context.defaultSharedPreferences.edit()
    val ciphertext = cipher.doFinal(encryptedSalt)
    val newSalt = (cipher.iv + ciphertext).toHex()
    historicalSalts.add(0, newSalt)
    edit.putString(Constants.Tip.SPEND_SALT, historicalSalts.joinToString())
    return edit.commit()
}

// for debug
internal fun readAllEncryptedSalts(context: Context): List<ByteArray> {
    val salts = context.defaultSharedPreferences.getString(Constants.Tip.SPEND_SALT, null) ?: return emptyList()
    val historicalSalts = salts.split(",")
    if (historicalSalts.isEmpty()) {
        return emptyList()
    }
    return historicalSalts.mapNotNull {
        if (it.isBlank()) return@mapNotNull null
        decryptSalt(it.trim().hexStringToByteArray())
    }
}

// for debug
internal fun deleteLatestSalts(context: Context): Boolean {
    val salts = context.defaultSharedPreferences.getString(Constants.Tip.SPEND_SALT, null) ?: return true
    val historicalSalts = salts.split(",")
    if (historicalSalts.isEmpty()) {
        return true
    }
    val edit = context.defaultSharedPreferences.edit()
    edit.putString(Constants.Tip.SPEND_SALT,  historicalSalts.drop(1).joinToString())
    return edit.commit()
}

internal fun clearSalts(context: Context = MixinApplication.appContext) {
    deleteKeyByAlias(Constants.Tip.ALIAS_SPEND_SALT)
    context.defaultSharedPreferences.remove(Constants.Tip.SPEND_SALT)
}

private fun decryptSalt(salt: ByteArray): ByteArray? {
    return runCatching {
        val iv = salt.slice(0..15).toByteArray()
        val ciphertext = salt.slice(16 until salt.size).toByteArray()
        val cipher = getDecryptCipher(Constants.Tip.ALIAS_SPEND_SALT, iv)
        cipher.doFinal(ciphertext)
    }.onFailure { clearSalts() }.getOrNull()
}