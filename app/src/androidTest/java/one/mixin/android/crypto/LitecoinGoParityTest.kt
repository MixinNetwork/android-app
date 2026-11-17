package one.mixin.android.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import blockchain.Blockchain
import one.mixin.android.Constants
import one.mixin.android.extension.hexString
import one.mixin.android.tip.bip44.Bip44Path
import org.bitcoinj.crypto.MnemonicCode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LitecoinGoParityTest {
    @Test
    fun bip84KeysAndAddressesMatchTipGo() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val addresses = mutableSetOf<String>()
        for (index in 0 until 10) {
            for (passphrase in listOf("", "TREZOR")) {
                val seed = MnemonicCode.toSeed(mnemonic.split(" "), passphrase)
                val derived = TipKeyGenerator.generate(seed, Constants.ChainId.Litecoin, index)
                addresses += derived.address
                val path = Bip44Path.litecoinSegwitPathString(index)
                assertEquals(derived.address, Blockchain.generateLitecoinAddress(seed.hexString(), path))
                assertEquals(derived.address, Blockchain.generateLitecoinAddressFromPrivateKey(derived.privateKey.hexString()))
                if (passphrase.isEmpty()) {
                    assertEquals(derived.address, Blockchain.generateLitecoinAddressFromMnemonic(mnemonic, path))
                    val wif = UtxoChainConfig.forChain(Constants.ChainId.Litecoin).wif.encode(derived.privateKey)
                    assertEquals(wif, Blockchain.exportLitecoinPrivateKey(mnemonic, path))
                    assertEquals(derived.address, Blockchain.generateLitecoinAddressFromPrivateKey(wif))
                }
            }
        }
        assertEquals(20, addresses.size)
    }

    @Test
    fun tipGoDefaultLitecoinPathIsBip84() {
        val mnemonic = "legal winner thank year wave sausage worth useful legal winner thank yellow"
        val seed = MnemonicCode.toSeed(mnemonic.split(" "), "")
        val derived = TipKeyGenerator.generate(seed, Constants.ChainId.Litecoin, 0)
        val wif = UtxoChainConfig.forChain(Constants.ChainId.Litecoin).wif.encode(derived.privateKey)

        assertEquals("m/84'/2'/0'/0/0", Bip44Path.litecoinSegwitPathString(0))
        assertEquals("ltc1qg0457yp54vvw74mdkrqhkvp2fyu387x2c8zs43", derived.address)
        assertEquals("T7VfwCYx5znSm3wn5vDmB6Gm4thZryoqwvH83T6WL6DK41mKJdx8", wif)
        assertEquals(derived.address, Blockchain.generateLitecoinAddress(seed.hexString(), ""))
        assertEquals(derived.address, Blockchain.generateLitecoinAddressFromMnemonic(mnemonic, ""))
        assertEquals(wif, Blockchain.exportLitecoinPrivateKey(mnemonic, ""))
        assertEquals(
            "ltc1qnfqhhq566zha2a8fxzjq9m3qcrd9xa4pdgnhpp",
            Blockchain.generateLitecoinAddressFromMnemonic(mnemonic, "m/44'/2'/0'/0/0"),
        )
    }
}
