package one.mixin.android.crypto.bip44

import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.tip.bip44.generateBip44Key
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.script.Script
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.web3j.crypto.Bip32ECKeyPair

@RunWith(RobolectricTestRunner::class)
class Bip44Test {

    @Test
    fun testBitcoinBip44() {
        val seed = "67f93560761e20617de26e0cb84f7234aaf373ed2e66295c3d7397e6d7ebe882ea396d5d293808b0defd7edd2babd4c091ad942e6a9351e6d075a29d4df872af"
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed.hexStringToByteArray())
        val basePath = intArrayOf(
            44 or Bip32ECKeyPair.HARDENED_BIT,
            0 or Bip32ECKeyPair.HARDENED_BIT,
            0 or Bip32ECKeyPair.HARDENED_BIT,
            0,
        )
        for (i in 0 until 20) {
            val path = basePath + i
            val keyPair = generateBip44Key(masterKeyPair, path)
            val ecKey = ECKey.fromPrivate(keyPair.privateKey)
            val address = Address.fromKey(NetworkParameters.fromID(NetworkParameters.ID_MAINNET), ecKey, Script.ScriptType.P2PKH)
            assert(address.toString() == testVectors[i])
        }
    }

    private val testVectors = mapOf(
        0 to "1AZnveys2k5taGCCF743RtrWGwc58UMeq",
        1 to "1AMYJTJyV4o1hwNACJtfdXBW6BiD1f5FXb",
        2 to "1NPFFtSiFRatoeUf35rwYb8j8C1u7sVhGa",
        3 to "1L44VTYEzWesp8cxnXcPGbUzuwTYoSW9at",
        4 to "1FK85vpZavzZu6oBCvBcmD4FWXQT5fVYRu",
        5 to "12QaHfWLtyuMwNXuap3FscMY434bw4TS6n",
        6 to "1NeFG5BYAR9bnjAG72SDYKvNZBH4kPa8r1",
        7 to "1yF3BiHqbQKL4aRfNYHQt4ZpgNagC4nQe",
        8 to "144vmUhuAZJsV3m2GsP5Kqp55Pmzwx2gna",
        9 to "1DQM5w6C7gNaCKBxQV3rXKftcamRKDPQ2M",
        10 to "17XRvBac5xpgMVr6LbsDA56fgsaAed4oEV",
        11 to "1BSQC3Qn38UT2WVfcM6LdybkfE7tTGW5M2",
        12 to "1KUG4EDePnG97xQNXtuU9Xmp4sThqFvSoS",
        13 to "18sXnPcBnXBRFBYbqr85aKPPNpwT4f52a8",
        14 to "15S2gpAVvprN1GPE44oXCdtkA4L7yQtBkX",
        15 to "1FvC2STfbj7dcr2ApAPhagnSCP5Dmy79nH",
        16 to "15VZHWTEjnQuJSvUHzS7K6gmYjNv4A5cVJ",
        17 to "1N4S7Z43gb22PDCcpjHhX25cgDSLxegdWm",
        18 to "1MzS2BktGqokVM4kDuB6VavjLuib72W2je",
        19 to "1GDLeWJ4FcK2uiTFvLshtVcBArA7M9ECxq",
    )
}
