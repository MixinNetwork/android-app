package one.mixin.android.crypto

import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys

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

    private fun deriveSolPrivateKeyAtIndex(masterKey: DeterministicKey, index: Int): ByteArray {
        val derivationPath = listOf(
            ChildNumber(44, true),   // m/44'
            ChildNumber(501, true),  // m/44'/501'
            ChildNumber(index, true),    // m/44'/501'/0'
            ChildNumber(0, true) // m/44'/501'/0'/index'
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
        return Keys.getAddress(ecKeyPair)
    }
}