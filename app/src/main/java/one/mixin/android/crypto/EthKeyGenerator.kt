package one.mixin.android.crypto

import blockchain.Blockchain
import one.mixin.android.extension.hexString
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.bip44.generateBip44Key
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric

object EthKeyGenerator {

    private fun deriveEthereumPrivateKeyAtIndex(masterKey: DeterministicKey, index: Int): ByteArray {
        val derivationPath = listOf(
            ChildNumber(44, true),   // m/44'
            ChildNumber(60, true),   // m/44'/60'
            ChildNumber(0, true),    // m/44'/60'/0'
            ChildNumber(0, false),   // m/44'/60'/0'/0
            ChildNumber(index, false)    // m/44'/60'/0'/0/0
        )

        var key = masterKey
        for (childNumber in derivationPath) {
            key = HDKeyDerivation.deriveChildKey(key, childNumber)
        }

        return key.privKeyBytes
    }

    fun getPrivateKeyFromMnemonic(mnemonic: String, passphrase: String = "", index: Int = 0): ByteArray? {
        val mnemonicWords = mnemonic.split(" ")
        MnemonicCode.INSTANCE.check(mnemonicWords)
        val seed = MnemonicCode.toSeed(mnemonicWords, passphrase)

        val masterKey = HDKeyDerivation.createMasterPrivateKey(seed)
        val privateKey = deriveEthereumPrivateKeyAtIndex(masterKey, index)
        return privateKey
    }

    fun privateKeyToAddress(privateKey: ByteArray): String {
        val ecKeyPair = ECKeyPair.create(privateKey)
        return Keys.toChecksumAddress(Keys.getAddress(ecKeyPair.publicKey))
    }

    internal fun deriveFromTipSeed(seed: ByteArray, index: Int): TipDerivedKey {
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        val keyPair = generateBip44Key(masterKeyPair, Bip44Path.ethereum(index))
        val privateKey = Numeric.toBytesPadded(keyPair.privateKey, 32)
        val address = Keys.toChecksumAddress(Keys.getAddress(keyPair.publicKey))
        val expectedAddress = Blockchain.generateEthereumAddress(seed.hexString(), Bip44Path.ethereumPathString(index))
        if (address != expectedAddress) {
            throw IllegalArgumentException("Generate illegal Ethereum Address")
        }
        return TipDerivedKey(privateKey, address)
    }
}
