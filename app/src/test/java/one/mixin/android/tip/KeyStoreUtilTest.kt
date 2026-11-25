package one.mixin.android.tip

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.StrongBoxUnavailableException
import androidx.test.core.app.ApplicationProvider
import one.mixin.android.MixinApplication
import one.mixin.android.crypto.EdKeyPair
import one.mixin.android.extension.base64Encode
import one.mixin.android.session.Session
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.InputStream
import java.io.OutputStream
import java.security.InvalidAlgorithmParameterException
import java.security.Key
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.UnrecoverableKeyException
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.util.Collections
import java.util.Date
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class KeyStoreUtilTest {
    @Before
    fun setUp() {
        MixinApplication.appContext = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(MixinApplication.appContext.packageManager).setSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE, true)
        keys.clear()
        requests.clear()
        lookupFails = false
        strongBoxFailure = null
        Security.addProvider(object : Provider("AndroidKeyStore", 1.0, "Test keystore") {
            init {
                put("KeyStore.AndroidKeyStore", TestKeyStore::class.java.name)
                put("KeyGenerator.AES", TestKeyGenerator::class.java.name)
            }
        })
    }

    @After
    fun tearDown() {
        Security.removeProvider("AndroidKeyStore")
    }

    @Test
    fun preservesExistingKeysAndLegacyCbcCiphertext() {
        val key = SecretKeySpec(ByteArray(32) { 1 }, "AES")
        keys["legacy"] = key
        val encrypt = getEncryptCipher("legacy")
        val plaintext = "existing-tip-or-salt".toByteArray()
        val ciphertext = encrypt.doFinal(plaintext)
        assertArrayEquals(plaintext, getDecryptCipher("legacy", encrypt.iv).doFinal(ciphertext))
        assertSame(key, getKeyByAlias("legacy"))
        assertEquals(emptyList<Boolean>(), requests)
    }

    @Test
    fun prefersStrongBoxForNewKeys() {
        val key = getKeyByAlias("new")
        assertSame(key, getKeyByAlias("new", createIfMissing = false))
        assertEquals(listOf(true), requests)
    }

    @Test
    fun fallsBackWhenStrongBoxGenerationIsUnavailable() {
        strongBoxFailure = StrongBoxUnavailableException("unavailable")
        getKeyByAlias("new")
        assertEquals(listOf(true, false), requests)
    }

    @Test
    fun fallsBackWhenStrongBoxRejectsParameters() {
        strongBoxFailure = InvalidAlgorithmParameterException("unsupported")
        getKeyByAlias("new")
        assertEquals(listOf(true, false), requests)
    }

    @Test
    fun usesOrdinaryKeystoreWhenStrongBoxIsAbsent() {
        shadowOf(MixinApplication.appContext.packageManager).setSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE, false)
        getKeyByAlias("new")
        assertEquals(listOf(false), requests)
    }

    @Test
    @Config(sdk = [26])
    fun supportsDevicesBeforeStrongBox() {
        getKeyByAlias("new")
        assertEquals(listOf(false), requests)
    }

    @Test
    fun missingDecryptionKeyOrLookupFailureNeverGeneratesAReplacement() {
        assertThrows(java.security.KeyStoreException::class.java) { getKeyByAlias("missing", createIfMissing = false) }
        lookupFails = true
        assertThrows(UnrecoverableKeyException::class.java) { getKeyByAlias("existing") }
        assertEquals(emptyList<Boolean>(), requests)
    }

    @Test
    fun failedSessionWritesCannotReturnStaleCachedKeys() {
        val context = MixinApplication.appContext
        val preferences = context.getSharedPreferences(Session.PREF_SESSION, Context.MODE_PRIVATE)
        var failCommit = false
        val failingPreferences = object : SharedPreferences by preferences {
            override fun edit(): SharedPreferences.Editor {
                val editor = preferences.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun clear(): SharedPreferences.Editor = apply { editor.clear() }
                    override fun commit(): Boolean {
                        editor.commit()
                        return !failCommit
                    }
                }
            }
        }
        MixinApplication.appContext = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                if (name == Session.PREF_SESSION) failingPreferences else super.getSharedPreferences(name, mode)
        }
        val oldKeyPair = EdKeyPair(ByteArray(32) { 1 }, ByteArray(32) { 2 })
        val newKeyPair = EdKeyPair(ByteArray(32) { 3 }, ByteArray(32) { 4 })
        Session.storeSessionKeys(oldKeyPair, "old-pin-token")
        assertSame(oldKeyPair, Session.getEd25519KeyPair())
        failCommit = true
        assertThrows(IllegalStateException::class.java) { Session.storeSessionKeys(newKeyPair, "new-pin-token") }
        assertThrows(IllegalStateException::class.java) { Session.getEd25519Seed() }
        assertThrows(IllegalStateException::class.java) { Session.getEd25519KeyPair() }
        failCommit = false
        assertEquals(newKeyPair.privateKey.base64Encode(), Session.getEd25519Seed())
        assertEquals("new-pin-token", Session.getPinToken())
        Session.storeSessionKeys(newKeyPair, "new-pin-token")
        failCommit = true
        assertThrows(IllegalStateException::class.java) { Session.clearAccount() }
        assertThrows(IllegalStateException::class.java) { Session.getEd25519Seed() }
        assertThrows(IllegalStateException::class.java) { Session.getEd25519KeyPair() }
    }

    class TestKeyGenerator : KeyGeneratorSpi() {
        private lateinit var spec: KeyGenParameterSpec
        private val strongBox: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && spec.isStrongBoxBacked

        override fun engineInit(params: AlgorithmParameterSpec, random: SecureRandom?) {
            spec = params as KeyGenParameterSpec
            requests += strongBox
            if (strongBox && strongBoxFailure is InvalidAlgorithmParameterException) {
                throw requireNotNull(strongBoxFailure)
            }
        }

        override fun engineInit(random: SecureRandom?) = error("Parameters required")
        override fun engineInit(keysize: Int, random: SecureRandom?) = error("Parameters required")

        override fun engineGenerateKey(): SecretKey {
            if (strongBox) strongBoxFailure?.let { throw it }
            return SecretKeySpec(ByteArray(32) { 2 }, "AES").also { keys[spec.keystoreAlias] = it }
        }
    }

    class TestKeyStore : KeyStoreSpi() {
        override fun engineGetKey(alias: String, password: CharArray?): Key? {
            if (lookupFails) throw UnrecoverableKeyException("unavailable")
            return keys[alias]
        }

        override fun engineContainsAlias(alias: String) = keys.containsKey(alias)
        override fun engineDeleteEntry(alias: String) { keys.remove(alias) }
        override fun engineAliases() = Collections.enumeration(keys.keys)
        override fun engineSize() = keys.size
        override fun engineIsKeyEntry(alias: String) = keys.containsKey(alias)
        override fun engineIsCertificateEntry(alias: String) = false
        override fun engineGetCertificateAlias(cert: Certificate?) = null
        override fun engineGetCertificate(alias: String): Certificate? = null
        override fun engineGetCertificateChain(alias: String): Array<Certificate>? = null
        override fun engineGetCreationDate(alias: String) = Date(0)
        override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit
        override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit
        override fun engineSetCertificateEntry(alias: String, cert: Certificate?) = error("Not used")
        override fun engineSetKeyEntry(alias: String, key: ByteArray?, chain: Array<out Certificate>?) = error("Not used")
        override fun engineSetKeyEntry(alias: String, key: Key?, password: CharArray?, chain: Array<out Certificate>?) = error("Not used")
    }

    companion object {
        private val keys = mutableMapOf<String, SecretKey>()
        private val requests = mutableListOf<Boolean>()
        private var lookupFails = false
        private var strongBoxFailure: Exception? = null
    }
}
