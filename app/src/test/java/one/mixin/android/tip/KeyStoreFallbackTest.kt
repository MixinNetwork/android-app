package one.mixin.android.tip

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.TipRequest
import one.mixin.android.api.request.TipSecretReadRequest
import one.mixin.android.api.request.TipSecretRequest
import one.mixin.android.api.response.TipEphemeral
import one.mixin.android.api.response.TipIdentity
import one.mixin.android.api.response.TipSecretResponse
import one.mixin.android.api.service.TipService
import one.mixin.android.crypto.aesEncrypt
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.session.Session
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import java.security.InvalidKeyException
import java.security.KeyStoreException

@RunWith(RobolectricTestRunner::class)
class KeyStoreFallbackTest {
    private lateinit var context: Context
    private var previousAppContext: Context? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        previousAppContext = ReflectionHelpers.getStaticField(MixinApplication::class.java, "appContext")
        MixinApplication.appContext = context
        context.defaultSharedPreferences.edit().clear().commit()
        context.getSharedPreferences(Session.PREF_SESSION, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(MixinApplication::class.java, "appContext", previousAppContext)
    }

    @Test
    fun getEncryptCipherPropagatesKeyStoreExceptionInsteadOfNullSecretKey() {
        val error =
            runCatching {
                getEncryptCipher(Constants.Tip.ALIAS_TIP_PRIV)
            }.exceptionOrNull()

        assertTrue(error is KeyStoreException)
        assertFalse(error is InvalidKeyException)
    }

    @Test
    fun storeEncryptedSaltReturnsFalseAndClearsPreferenceWhenKeyStoreFails() {
        context.defaultSharedPreferences.putString(Constants.Tip.SPEND_SALT, "deadbeef")

        val stored = storeEncryptedSalt(context, ByteArray(16) { 1 })

        assertFalse(stored)
        assertNull(context.defaultSharedPreferences.getString(Constants.Tip.SPEND_SALT, null))
    }

    @Test
    fun storeTipPrivSafelyDoesNotThrowAndClearsPreferenceWhenKeyStoreFails() {
        context.defaultSharedPreferences.putString(Constants.Tip.TIP_PRIV, "deadbeef")

        storeTipPrivSafely(context, ByteArray(32) { 2 })

        assertNull(context.defaultSharedPreferences.getString(Constants.Tip.TIP_PRIV, null))
    }

    @Test
    fun ephemeralGetSeedRecoversAndReturnsSeedWhenKeyStoreFails() =
        runBlocking {
            val pinToken = ByteArray(32) { (it + 1).toByte() }
            val expectedSeed = ByteArray(32) { (it + 10).toByte() }
            Session.storePinToken(pinToken.base64Encode())
            context.defaultSharedPreferences.putString(Constants.Tip.EPHEMERAL_SEED, "deadbeef")

            val encryptedSeed = aesEncrypt(pinToken, expectedSeed).base64RawURLEncode()
            val fakeService =
                object : TipService {
                    override suspend fun tipIdentity(): MixinResponse<TipIdentity> =
                        error("unused")

                    override suspend fun tipEphemerals(): MixinResponse<List<TipEphemeral>> =
                        MixinResponse<List<TipEphemeral>>().apply {
                            data =
                                listOf(
                                    TipEphemeral(
                                        type = "ephemeral",
                                        deviceId = "device-1",
                                        userId = "user-1",
                                        seedBase64 = encryptedSeed,
                                        createdAt = "2026-01-01T00:00:00Z",
                                    ),
                                )
                        }

                    override suspend fun tipEphemeral(request: TipRequest): MixinResponse<Unit> =
                        MixinResponse<Unit>().apply { data = Unit }

                    override suspend fun readTipSecret(request: TipSecretReadRequest): MixinResponse<TipSecretResponse> =
                        error("unused")

                    override suspend fun updateTipSecret(request: TipSecretRequest): MixinResponse<Unit> =
                        error("unused")
                }

            val seed = Ephemeral(fakeService).getEphemeralSeed(context, "device-1")

            assertArrayEquals(expectedSeed, seed)
            assertNull(context.defaultSharedPreferences.getString(Constants.Tip.EPHEMERAL_SEED, null))
        }
}
