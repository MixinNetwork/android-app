package one.mixin.android.crypto

import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.bip44.generateBip44Key
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.base.ScriptType
import org.bitcoinj.crypto.ECKey
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.utils.Numeric
import java.math.BigInteger

object BitcoinKeyGenerator {

    fun getPrivateKeyFromMnemonic(mnemonic: String, passphrase: String = "", index: Int = 0): ByteArray {
        val mnemonicWords: List<String> = mnemonic.split(" ")
        MnemonicCode.INSTANCE.check(mnemonicWords)
        val seed: ByteArray = MnemonicCode.toSeed(mnemonicWords, passphrase)
        val masterKeyPair: Bip32ECKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        val bip84KeyPair: Bip32ECKeyPair = generateBip44Key(masterKeyPair, Bip44Path.bitcoinSegwit(index))
        return Numeric.toBytesPadded(bip84KeyPair.privateKey, 32)
    }

    fun privateKeyToAddress(privateKey: ByteArray): String {
        val ecKey: ECKey = ECKey.fromPrivate(BigInteger(1, privateKey), true)
        return ecKey.toAddress(ScriptType.P2WPKH, BitcoinNetwork.MAINNET).toString()
    }

    fun mnemonicToAddress(mnemonic: String, passphrase: String = "", index: Int = 0): String {
        val privateKey: ByteArray = getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
        return privateKeyToAddress(privateKey)
    }
}
