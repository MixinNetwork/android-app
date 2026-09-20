package one.mixin.android.session

import android.content.SharedPreferences
import okio.ByteString.Companion.decodeHex
import one.mixin.android.extension.toHex
import one.mixin.android.tip.getKeyByAlias
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class SessionSecretPreferences(
    private val preferences: SharedPreferences,
    private val getKey: (Boolean) -> SecretKey = { getKeyByAlias("mixin_session_secrets", createIfMissing = it) },
) {
    private var pendingCommit = false

    @Synchronized
    fun migrate() {
        if (pendingCommit) commit(preferences.edit())
        val legacyKeys = KEYS.filter { preferences.contains(it) }
        if (legacyKeys.isEmpty()) return

        val editor = preferences.edit()
        for (key in legacyKeys) {
            val encryptedKey = "$key.encrypted"
            val encrypted = preferences.getString(encryptedKey, null)
            if (encrypted != null) {
                decrypt(key, encrypted)
            } else {
                val value = requireNotNull(preferences.getString(key, null))
                editor.putString(encryptedKey, encrypt(key, value))
            }
            editor.remove(key)
        }
        commit(editor)
    }

    @Synchronized
    fun getString(key: String): String? {
        migrate()
        val encrypted = preferences.getString("$key.encrypted", null) ?: return null
        return decrypt(key, encrypted)
    }

    @Synchronized
    fun putStrings(values: Map<String, String>) {
        if (pendingCommit) commit(preferences.edit())
        val editor = preferences.edit()
        for ((key, value) in values) {
            editor.putString("$key.encrypted", encrypt(key, value)).remove(key)
        }
        commit(editor)
    }

    @Synchronized
    fun clear() {
        commit(preferences.edit().clear())
    }

    private fun commit(editor: SharedPreferences.Editor) {
        // A failed commit can still change SharedPreferences in memory; retry before returning secrets.
        pendingCommit = true
        check(editor.commit()) { "Failed to persist session secrets" }
        pendingCommit = false
    }

    private fun encrypt(key: String, value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKey(KEYS.none { preferences.contains("$it.encrypted") }))
        cipher.updateAAD(key.toByteArray())
        val encrypted = (cipher.iv + cipher.doFinal(value.toByteArray())).toHex()
        check(decrypt(key, encrypted) == value) { "Failed to verify session secret" }
        return encrypted
    }

    private fun decrypt(key: String, value: String): String {
        val bytes = value.decodeHex().toByteArray()
        require(bytes.size >= 28) { "Invalid encrypted session secret" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getKey(false), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        cipher.updateAAD(key.toByteArray())
        return cipher.doFinal(bytes, 12, bytes.size - 12).toString(Charsets.UTF_8)
    }

    companion object {
        const val PREF_PIN_TOKEN = "pref_pin_token"
        const val PREF_NAME_TOKEN = "pref_name_token"
        const val PREF_ED25519_PRIVATE_KEY = "pref_ed25519_private_key"
        private val KEYS = listOf(PREF_PIN_TOKEN, PREF_NAME_TOKEN, PREF_ED25519_PRIVATE_KEY)
    }
}
