package one.mixin.android.util

import one.mixin.android.crypto.privateKeyToCurve25519
import one.mixin.android.crypto.publicKeyToCurve25519
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toHex
import java.util.zip.CRC32
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CryptoUtilTest {

    @Test
    fun `test curve25519 conversion`() {
        val public = byteArrayOf(
            147.toByte(),
            193.toByte(),
            19.toByte(),
            201.toByte(),
            96.toByte(),
            200.toByte(),
            216.toByte(),
            248.toByte(),
            19.toByte(),
            54.toByte(),
            49.toByte(),
            150.toByte(),
            150.toByte(),
            167.toByte(),
            41.toByte(),
            75.toByte(),
            87.toByte(),
            242.toByte(),
            28.toByte(),
            199.toByte(),
            153.toByte(),
            217.toByte(),
            6.toByte(),
            224.toByte(),
            84.toByte(),
            169.toByte(),
            210.toByte(),
            80.toByte(),
            186.toByte(),
            202.toByte(),
            128.toByte(),
            201.toByte(),
        )
        val seed = byteArrayOf(
            126.toByte(),
            51.toByte(),
            73.toByte(),
            128.toByte(),
            30.toByte(),
            5.toByte(),
            236.toByte(),
            244.toByte(),
            27.toByte(),
            127.toByte(),
            26.toByte(),
            150.toByte(),
            49.toByte(),
            250.toByte(),
            179.toByte(),
            252.toByte(),
            107.toByte(),
            36.toByte(),
            94.toByte(),
            118.toByte(),
            231.toByte(),
            79.toByte(),
            230.toByte(),
            175.toByte(),
            74.toByte(),
            217.toByte(),
            163.toByte(),
            61.toByte(),
            162.toByte(),
            214.toByte(),
            235.toByte(),
            156.toByte(),
        )
        val targetPrivate = byteArrayOf(
            232.toByte(),
            23.toByte(),
            164.toByte(),
            168.toByte(),
            212.toByte(),
            159.toByte(),
            250.toByte(),
            121.toByte(),
            48.toByte(),
            244.toByte(),
            252.toByte(),
            13.toByte(),
            183.toByte(),
            100.toByte(),
            82.toByte(),
            162.toByte(),
            219.toByte(),
            106.toByte(),
            10.toByte(),
            171.toByte(),
            30.toByte(),
            240.toByte(),
            31.toByte(),
            208.toByte(),
            91.toByte(),
            201.toByte(),
            15.toByte(),
            179.toByte(),
            136.toByte(),
            192.toByte(),
            210.toByte(),
            87.toByte(),
        )
        val targetPublic = byteArrayOf(
            159.toByte(),
            128.toByte(),
            169.toByte(),
            96.toByte(),
            138.toByte(),
            29.toByte(),
            242.toByte(),
            209.toByte(),
            248.toByte(),
            250.toByte(),
            1.toByte(),
            148.toByte(),
            133.toByte(),
            194.toByte(),
            107.toByte(),
            237.toByte(),
            154.toByte(),
            18.toByte(),
            40.toByte(),
            50.toByte(),
            51.toByte(),
            58.toByte(),
            81.toByte(),
            213.toByte(),
            200.toByte(),
            152.toByte(),
            8.toByte(),
            126.toByte(),
            7.toByte(),
            140.toByte(),
            6.toByte(),
            47.toByte(),
        )

        val curve25519PrivateKey = privateKeyToCurve25519(seed)
        assert(curve25519PrivateKey.contentEquals(targetPrivate))
        val curve25519PublicKey = publicKeyToCurve25519(public)
        assert(curve25519PublicKey.contentEquals(targetPublic))
    }

    @Test
    fun `test sha3-256`() {
        val m = "Hello, world!".toByteArray()
        val a = "Hello"
        val b = ","
        val c = " world"
        val d = "!"
        val digest1 = m.sha3Sum256()
        val digest2 =
            (a.toByteArray() + b.toByteArray() + c.toByteArray() + d.toByteArray()).sha3Sum256()
        println("digest1: ${digest1.toHex()}")
        println("digest2: ${digest2.toHex()}")
        assertEquals(
            "f345a219da005ebe9c1a1eaad97bbf38a10c8473e41d0af7fb617caa0c6aa722",
            digest1.toHex(),
        )
    }

    @Test
    fun `test sha3-256 hash tip priv`() {
        var targetHex =
            "3039326262663461613432633963643336376239663465383363353861393031363539353037316465636261386436646330633437643565613862326363633535485841484668386b6859424757413256336f5555765841583461576e517345794e7a4b6f41334c6e4a6b78744b514e686357536834537774373261316277376147387554673946333179627a534a79754e48454e554274476f62556648624b4e505559596b486e6875507457737a6143754e4a336e42785a34437274385138416d4a32665a7a6e4c783345444d32457166363364724e6d573656566d6d7a42515563344e324a61587a46747434484646577476556b"
        var result = targetHex.hexStringToByteArray().sha3Sum256()
        println("sha3-256: ${result.toHex()}")
        assertEquals(
            "25c945f52d5742aa6c3d26edaab75113cb59e824e7ff5a42f71da86800c7ce14",
            result.toHex(),
        )
        assertTrue(
            "25c945f52d5742aa6c3d26edaab75113cb59e824e7ff5a42f71da86800c7ce14".hexStringToByteArray()
                .contentEquals(result),
        )

        targetHex =
            "26a349a968b8ebdf8d27b8f855abb67d797c0de3468cba932ba9a3fde967acd386de43e9dab0430998a43a34efe8f8608cac0f77de5981bfa8cda7ab35b1248c"
        result = targetHex.hexStringToByteArray().sha3Sum256()
        println("sha3-256: ${result.toHex()}")
        assertEquals(
            "3b79832a520ae9e1263fa5b28023e4e944a5b5920a6995a865b33306b68e788f",
            result.toHex(),
        )
        assertTrue(
            "3b79832a520ae9e1263fa5b28023e4e944a5b5920a6995a865b33306b68e788f".hexStringToByteArray()
                .contentEquals(result),
        )
    }

    @Test
    fun `test RCR`() {
        val list = listOf(
            "yHm7QnW2Rp",
            "4fEwLdG8tK",
            "J9uX6vZpNc",
            "5VhQxPbUaS",
            "A2kDlTjRgM",
        )
        val result = listOf<Long>(
            3806008316,
            2006416362,
            4072379794,
            1074432487,
            69700325,
        )
        for ((index, value) in list.withIndex()) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            val crc = calculateCrc32(bytes)
            println(crc)
            assertEquals(result[index], crc)
        }
    }

    private fun calculateCrc32(bytes: ByteArray): Long {
        val crc = CRC32()
        crc.update(bytes)
        return crc.value
    }
}
