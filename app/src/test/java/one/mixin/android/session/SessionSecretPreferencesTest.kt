package one.mixin.android.session

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.session.SessionSecretPreferences.Companion.PREF_ED25519_PRIVATE_KEY
import one.mixin.android.session.SessionSecretPreferences.Companion.PREF_NAME_TOKEN
import one.mixin.android.session.SessionSecretPreferences.Companion.PREF_PIN_TOKEN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyStoreException
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class SessionSecretPreferencesTest {
    private lateinit var preferences: SharedPreferences
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val legacyValues = mapOf(
        PREF_ED25519_PRIVATE_KEY to "test-session-seed",
        PREF_PIN_TOKEN to "test-pin-token",
        PREF_NAME_TOKEN to "test-legacy-rsa-private-key",
    )

    @Before
    fun setUp() {
        preferences = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("session-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
    }

    @Test
    fun migratesAllSecretsAndCanReadThemAgainWithoutChangingAccountMetadata() {
        val editor = preferences.edit().putString("pref_name_account", "account")
        legacyValues.forEach { (name, value) -> editor.putString(name, value) }
        editor.commit()

        val storage = SessionSecretPreferences(preferences) { key }
        assertEquals(legacyValues[PREF_PIN_TOKEN], storage.getString(PREF_PIN_TOKEN))
        legacyValues.forEach { (name, value) ->
            assertFalse(preferences.contains(name))
            assertNotEquals(value, preferences.getString("$name.encrypted", null))
        }
        assertEquals("account", preferences.getString("pref_name_account", null))

        val saved = preferences.all
        val reopened = SessionSecretPreferences(preferences) { key }
        reopened.migrate()
        legacyValues.forEach { (name, value) -> assertEquals(value, reopened.getString(name)) }
        assertEquals(saved, preferences.all)
    }

    @Test
    fun newWritesAreEncryptedAndUseFreshIvs() {
        val storage = SessionSecretPreferences(preferences) { key }
        storage.putStrings(legacyValues)
        val first = preferences.all
        storage.putStrings(legacyValues)
        legacyValues.forEach { (name, value) ->
            assertFalse(preferences.contains(name))
            assertNotEquals(first["$name.encrypted"], preferences.getString("$name.encrypted", null))
            assertEquals(value, storage.getString(name))
        }
        storage.clear()
        legacyValues.keys.forEach { assertNull(storage.getString(it)) }
    }

    @Test
    fun failedEncryptionLeavesAllLegacyValuesUntouched() {
        legacyValues.forEach { (name, value) -> preferences.edit().putString(name, value).commit() }
        val before = preferences.all
        var encryptions = 0
        val storage = SessionSecretPreferences(preferences) { create ->
            if (create && ++encryptions == 2) throw KeyStoreException("unavailable")
            key
        }
        assertThrows(KeyStoreException::class.java) { storage.migrate() }
        assertEquals(before, preferences.all)
        SessionSecretPreferences(preferences) { key }.migrate()
        legacyValues.keys.forEach { assertFalse(preferences.contains(it)) }
    }

    @Test
    fun failedSessionReplacementLeavesBothOldSecretsUntouched() {
        SessionSecretPreferences(preferences) { key }.putStrings(legacyValues)
        val before = preferences.all
        var keyReads = 0
        val storage = SessionSecretPreferences(preferences) {
            if (++keyReads == 3) throw KeyStoreException("unavailable")
            key
        }
        assertThrows(KeyStoreException::class.java) {
            storage.putStrings(mapOf(PREF_ED25519_PRIVATE_KEY to "new-seed", PREF_PIN_TOKEN to "new-token"))
        }
        assertEquals(before, preferences.all)
    }

    @Test
    fun failedCommitIsRetriedBeforeReturningAnInMemorySecret() {
        preferences.edit().putString(PREF_PIN_TOKEN, "legacy").commit()
        var failCommit = true
        var commits = 0
        val failingPreferences = object : SharedPreferences by preferences {
            override fun edit(): SharedPreferences.Editor {
                val editor = preferences.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun remove(key: String?): SharedPreferences.Editor = apply { editor.remove(key) }
                    override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { editor.putString(key, value) }
                    override fun commit(): Boolean {
                        commits++
                        editor.commit()
                        return !failCommit
                    }
                }
            }
        }
        val storage = SessionSecretPreferences(failingPreferences) { key }
        assertThrows(IllegalStateException::class.java) { storage.migrate() }
        assertFalse(preferences.contains(PREF_PIN_TOKEN))
        assertThrows(IllegalStateException::class.java) { storage.getString(PREF_PIN_TOKEN) }
        failCommit = false
        assertEquals("legacy", storage.getString(PREF_PIN_TOKEN))
        assertEquals(3, commits)
    }

    @Test
    fun migrationCanRetryAfterAnUnwrittenCommit() {
        preferences.edit().putString(PREF_PIN_TOKEN, "legacy").commit()
        val before = preferences.all
        val failingPreferences = object : SharedPreferences by preferences {
            override fun edit(): SharedPreferences.Editor {
                val editor = preferences.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun commit() = false
                }
            }
        }
        assertThrows(IllegalStateException::class.java) { SessionSecretPreferences(failingPreferences) { key }.migrate() }
        assertEquals(before, preferences.all)
        assertEquals("legacy", SessionSecretPreferences(preferences) { key }.getString(PREF_PIN_TOKEN))
        assertFalse(preferences.contains(PREF_PIN_TOKEN))
    }

    @Test
    fun tamperedOrSwappedCiphertextNeverFallsBackToPlaintext() {
        val storage = SessionSecretPreferences(preferences) { key }
        storage.putStrings(legacyValues)
        val encryptedName = "$PREF_PIN_TOKEN.encrypted"
        val encrypted = requireNotNull(preferences.getString(encryptedName, null))
        val modified = encrypted.dropLast(1) + if (encrypted.last() == '0') "1" else "0"
        preferences.edit().putString(encryptedName, modified).putString(PREF_PIN_TOKEN, "legacy").commit()
        val before = preferences.all
        assertThrows(AEADBadTagException::class.java) { storage.getString(PREF_PIN_TOKEN) }
        assertEquals(before, preferences.all)

        preferences.edit().putString(encryptedName, preferences.getString("$PREF_ED25519_PRIVATE_KEY.encrypted", null)).commit()
        assertThrows(AEADBadTagException::class.java) { storage.getString(PREF_PIN_TOKEN) }
        assertEquals("legacy", preferences.getString(PREF_PIN_TOKEN, null))
    }

    @Test
    fun missingKeyDoesNotGenerateOrRemoveStoredSecrets() {
        SessionSecretPreferences(preferences) { key }.putStrings(legacyValues)
        val before = preferences.all
        val storage = SessionSecretPreferences(preferences) { create ->
            assertFalse(create)
            throw KeyStoreException("missing")
        }
        assertThrows(KeyStoreException::class.java) { storage.getString(PREF_PIN_TOKEN) }
        assertThrows(KeyStoreException::class.java) { storage.putStrings(mapOf(PREF_PIN_TOKEN to "replacement")) }
        assertEquals(before, preferences.all)
    }

    @Test
    fun malformedCiphertextIsRejectedAndValidEncryptedValueTakesPrecedence() {
        val storage = SessionSecretPreferences(preferences) { key }
        listOf("0", "zz", "00").forEach { invalid ->
            preferences.edit().putString("$PREF_PIN_TOKEN.encrypted", invalid).commit()
            assertThrows(IllegalArgumentException::class.java) { storage.getString(PREF_PIN_TOKEN) }
        }
        storage.putStrings(mapOf(PREF_PIN_TOKEN to "current"))
        preferences.edit().putString(PREF_PIN_TOKEN, "stale").commit()
        assertEquals("current", storage.getString(PREF_PIN_TOKEN))
        assertFalse(preferences.contains(PREF_PIN_TOKEN))
    }
}
