package one.mixin.android.tip

import android.content.Context
import one.mixin.android.Constants
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toHex

internal fun readEncryptedSalt(context: Context): ByteArray? {
    val salt = context.defaultSharedPreferences.getString(Constants.Tip.SPEND_SALT, null)?.hexStringToByteArray() ?: return null

    val iv = salt.slice(0..15).toByteArray()
    val ciphertext = salt.slice(16 until salt.size).toByteArray()
    val cipher = getDecryptCipher(Constants.Tip.ALIAS_SPEND_SALT, iv)
    return cipher.doFinal(ciphertext)
}

internal fun storeEncryptedSalt(context: Context, encryptedSalt: ByteArray): Boolean {
    val cipher = getEncryptCipher(Constants.Tip.ALIAS_SPEND_SALT)
    val edit = context.defaultSharedPreferences.edit()
    val ciphertext = cipher.doFinal(encryptedSalt)
    edit.putString(Constants.Tip.SPEND_SALT, (cipher.iv + ciphertext).toHex())
    return edit.commit()
}