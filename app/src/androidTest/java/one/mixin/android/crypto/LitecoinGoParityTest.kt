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
        for (index in listOf(0, 1)) {
            for (passphrase in listOf("", "TREZOR")) {
                val seed = MnemonicCode.toSeed(mnemonic.split(" "), passphrase)
                val derived = TipKeyGenerator.generate(seed, Constants.ChainId.Litecoin, index)
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
    }
}
