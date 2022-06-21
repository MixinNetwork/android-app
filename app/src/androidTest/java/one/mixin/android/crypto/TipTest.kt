package one.mixin.android.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import crypto.Crypto
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.api.request.TipSignData
import one.mixin.android.extension.base64RawUrlDecode
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toBeByteArray
import one.mixin.android.extension.toHex
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.days

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TipTest {

    @Test
    fun testTipGuard() {
        val suite = Crypto.newSuiteBn256()
        val signer = suite.scalar()
        signer.setBytes("5HXAHFh8khYBGWA2V3oUUvXAX4aWnQsEyNzKoA3LnJkxtKQNhcWSh4Swt72a1bw7aG8uTg9F31ybzSJyuNHENUBtGobUfHbKNPUYYkHnhuPtWszaCuNJ3nBxZ4Crt8Q8AmJ2fZznLx3EDM2Eqf63drNmW6VVmmzBQUc4N2JaXzFtt4HFFWtvUk".base64RawUrlDecode())
        val user = suite.scalar()
        user.setBytes("p8ogX1BMb-IsRisEBS2kOchXEqjbqxtsXR8J9Bf0AGI".base64RawUrlDecode())
        val ephemeral = suite.scalar()
        ephemeral.setBytes("55ffbc2955b39a1ddb44aa5675a395ceb3766d59f11b8cf865651f72a281322e".hexStringToByteArray())
        val eBytes = ephemeral.privateKeyBytes()

        val nonce = 1024L
        val nonceBytes = nonce.toBeByteArray()
        println("nonce: ${nonceBytes.toHex()}")
        val grace = 128.days.inWholeNanoseconds
        val graceBytes = grace.toBeByteArray()
        println("grace: ${graceBytes.toHex()}")

        val sPk = signer.publicKey()
        println("sPK: ${sPk.publicKeyString()}")
        val uPk = user.publicKey()
        val pKeyBytes = uPk.publicKeyBytes()
        val msg = pKeyBytes + eBytes + nonceBytes + graceBytes
        println("msg: ${msg.toHex()}")

        val sig = user.sign(msg)
        println("sig: ${sig.toHex()}")

        val data = TipSignData(
            identity = uPk.publicKeyString(),
            ephemeral = eBytes.toHex(),
            nonce = nonce,
            grace = grace,
        )
        val json = Gson().toJson(data).toByteArray()

        val cipher = Crypto.encrypt(sPk, user, json)
        println("cipher: ${cipher.toHex()}")

        val plain = Crypto.decrypt(uPk, signer, cipher)
        assert(json.contentEquals(plain))
    }
}