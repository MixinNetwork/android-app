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
        signer.setBytes("0da58ccc3b323d92af281367333f4c120418ed2700de803046947f59707b3479".hexStringToByteArray())
        val user = suite.scalar()
        assert(signer.publicKey().publicKeyString() == "5HSsddpV8HiKbu9vL3ZB69dtDjaZdQAn8RuL2aK1d1yZknUhBAXNhJLkZfCc2RwTxcaxKonNsXnQJFGcM8jgBztGTHzCA26LgKZWCe74Bw8VJ51FyqCGTysSLnNvkKPT3gh1RhjbyKPEoq3d3DXhJEQJt7GhVgZC82VeMfME9LnYECn9Pui1ta")

        user.setBytes("p8ogX1BMb-IsRisEBS2kOchXEqjbqxtsXR8J9Bf0AGI".base64RawUrlDecode())
        val ephemeral = suite.scalar()
        ephemeral.setBytes("-e7M3ZD5k-rW6KQ7GVfV9V9bpmfbUY5y8HiqqBGv8-r46YMRRSlyc-ZKGU3s92gsC9GVuIhgn33I".base64RawUrlDecode())
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
        println("uSk: ${user.privateKeyBytes().toHex()}")
        println("uPk: ${uPk.publicKeyString()}")
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
        println("data: ${Gson().toJson(data)}")
        val json = Gson().toJson(data).toByteArray()
        println("json: ${json.toHex()}")

        val cipher = Crypto.encrypt(sPk, user, json)
        println("cipher: ${cipher.toHex()}")

        val plain = Crypto.decrypt(uPk, signer, cipher)
        assert(json.contentEquals(plain))
    }
}