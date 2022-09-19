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
        val partials = "000672f4fa337710610a029386db47f0f04073ed83445635f3083cff2fa2fc798a7a078444a62a12a7343a5bdc23d97e4ed0b509802592470c9cbadaaf55088c7cce,00001730715ccff97f66377ce70f17b16cd386ff64c1cff19402cc6cd0af63fdd625469143499e0101795f3943c3edfb7e1cf5a8e599b078afd2beb8ebde99773357,00010d9e382e3d3145598b0ac0558fb573c955d60e35d8d30024d6c76233528988b41f47e3182376b7c77df842bc012eff977293c026a7e6921a4952907cd5e07ef3,0002589a9f3d8f92111fdf32e3c542279aac9a4fdcf056822166166cdcda7af71ab222b566ff10b2d0596944564f7f4b2189bae64ceaa882f87301806b4f98a77d01,00030a5d291188c798f8ba816cbd980bbaef10abdf3bd2679109adbc8fef0294b25a1b0b8053a0332c889c49ebee02ac17d3518fda805b76f9302d7998eae6710262,00047d0924c11c91fa11af803328c4ea3bc22dc0d1b5513327dfba97031a50f32c966ee99a618808b2ffa75a8ba860e9a72d3f7733765a193f206d1e43a143f23050,0005005047f969b9ebf101beeb4280f6a281ba5391b91000971f960e006ca3c7ad4f5f2e871b8d3ae83ca9d485d4a499be144f0f1ca0b479a84e1fbb4f3aaaa81426"
        val commitments = "5HYSNEcudZqucSo8tjVkkkuz6QiTQPSCjSxdGY9gZ1V3JKGtJ5bfZXS3QbB8AnBPLVrJQLEyJaucw4S6MmJhkwTpqCpTiovjiiaab4PUZsS8NBUBFharae9R3QUMR9ouPTgFxqmqMGGXSAqrRziSqneTcEkgLymx5oahxGfSeovgTN1FDgKiAt,5Jr6mXiEF1xtR7jJ8o2923vsGcqnjnegK9eVybDfXCnFXKhRVaJDCMHvGuHLo92TWSuBVrwDxYM8nHDiqYVb7csPQHHTyPjrGMPZiPe3PFhBYpz5nsDeVxjknZ8C9fPYYi67qyBy4fy1T6U4itXQEzjCTBdtjw2TXrNKe5oYPvJWx3X6ZpygCi,5JTnSpeBG9NyBAEkXL6KxFW3fYMhM4rgBqLMXynrBxHcshuQViedG2H4UumcdLZzMjkyyAdsGmEAf4MKiULmN93aaNvqCXiHH7MYnyvGp96s9bXs2M61HgH1KKZ4Tvk6yBfnVmvwmGVnnsQMd7fTky37VZGqn56SkQa7ANEz52Bwfs7uBBVZzV,5JDXsF4qPcf9cwKuAL2H4scnaMEz3GYnyM3koVx5AE7rDvaLCeqWZ8ksXES9eQooKtfQHZyxhhFZumrQqkMqsbxatYSvZJFSWtTESoBqGSqb9F2pvQboik1uJyw7VNrDFUkvVj64JXY6cThHvWQpK96zqELurNjfEPjNRB79c5ESqqfK4FXtce,5Jr8U6gPzoi9iYLRY4bVJBbYfsk95xM7AUyMhrsXzfG16nzkzcR2LpEsPFeYwS75WCDMGVta55SDWjb1cRPmWVKrKvr9A8RKRJkF3yop9gc2Kia4bccYqH1QcnNZAGoDLcqHiLSgbWwvjb1NaS2vVtUHsfnbKAQ64jX96gSMJis39UEh9HJiiA"
        val assignor = "5445292d1a1095b708d81921cd49060798bcafffee9cea829da3bc0b323ddcc75683a5fe449e6e4def8ceb628af0dd1fb459288472dbe1a6405e6bdbfefe5bc03311e0ab8e257faf5d6985f4fb8355deba1b922c4ce9f24f68dfd32311b4e5db5ae4a0a2366a47f795f36a0ca8eb24166991406058be7f30c19db48ef7974889".hexStringToByteArray()
        val aggSig = Crypto.recoverSignature(partials, commitments, assignor, 7)
        println("aggSig ${aggSig.toHex()}")
    }
}
