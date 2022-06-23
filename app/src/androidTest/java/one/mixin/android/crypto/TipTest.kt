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

    @Test
    fun testAggregate() {
        val sigs = "478bb4dd84f453bfd7a1f947a22d22ca30356989742ea34bc0199183c1da29b82c753373c397c441f709326358708c29f4724822caf77d81114129aa644fd5e9,133cd21260e486daa3074cb5ac4a01118affcef4fa5f0aafa45ea3acacecb8a37a66f058882e821cec997eaa29be6dc309b953fb709497c3adb1e57915df25f1,23a9b2cfdcbf514f2f34b1ea3b3c6cd033f44e409d3e185c8b6e74885e887a6b325bb0002dcbd07a908bc78468df4697525854e697ab5455b3ea353225eeb613,604397b158528063bf4522bbfb99a99188e1bba899ece8fa68638f7582b024c1823c1bfaefe72e8b17e90e7b2496d8422aa633249f925ffd69e74dbe28436f25,088242218b103d3f8d0f0ab42563272f69d78d28451c53f9353e4849e31205596d9db1ac1e4be887b34ed6c580d54da0b7a3003e29e9edefa743f9b61703ebbe,7ea98e9a48d333ec8bcd5c3673531fabb289725fe7490b9b539f28764d0be1f83cc8c37c611c444bb5b5a8bd689b60923460fe2a4daec977a8c9f297b178b834,3c9c70161e18a2669fec1926f6210376f428b62bd507bdf3f0af835689fda7450ae770c59605f6e6c9bf2857283cf0bba49a67670784ae61043e898d162444c7"
        val aggSig = Crypto.aggregateSignatures(sigs)
        println("aggSig ${aggSig.toHex()}")
    }
}