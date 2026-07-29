package one.mixin.android.crypto

import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.bip44.generateBip44Key
import one.mixin.android.util.encodeToBase58WithChecksum
import org.bitcoinj.crypto.MnemonicCode
import org.json.JSONArray
import org.json.JSONObject
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.nio.charset.StandardCharsets

object TronKeyGenerator {
    private const val ADDRESS_PREFIX = 0x41
    private const val MESSAGE_PREFIX = "\u0019TRON Signed Message:\n"

    fun getPrivateKeyFromMnemonic(
        mnemonic: String,
        passphrase: String = "",
        index: Int = 0,
    ): ByteArray {
        val words = mnemonic.split(" ")
        MnemonicCode.INSTANCE.check(words)
        val seed = MnemonicUtils.generateSeed(mnemonic, passphrase)
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        val keyPair = generateBip44Key(masterKeyPair, Bip44Path.tron(index))
        return Numeric.toBytesPadded(keyPair.privateKey, 32)
    }

    fun privateKeyToAddress(privateKey: ByteArray): String {
        return addressPayload(privateKey).encodeToBase58WithChecksum()
    }

    internal fun deriveFromTipSeed(seed: ByteArray, index: Int): TipDerivedKey {
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        val keyPair = generateBip44Key(masterKeyPair, Bip44Path.tron(index))
        val privateKey = Numeric.toBytesPadded(keyPair.privateKey, 32)
        return TipDerivedKey(privateKey, privateKeyToAddress(privateKey))
    }

    fun signMessageV2(privateKey: ByteArray, message: String): String {
        return signMessageV2(privateKey, message.toByteArray(StandardCharsets.UTF_8))
    }

    fun signMessageV2(privateKey: ByteArray, messageBytes: ByteArray): String {
        val prefix = MESSAGE_PREFIX.toByteArray(StandardCharsets.UTF_8)
        val size = messageBytes.size.toString().toByteArray(StandardCharsets.UTF_8)
        return Numeric.toHexString(signDigest(privateKey, Hash.sha3(prefix + size + messageBytes)))
    }

    fun signTransaction(privateKey: ByteArray, transactionJson: String): String {
        val transaction = JSONObject(transactionJson)
        val txId = Numeric.hexStringToByteArray(transaction.getString("txID"))
        require(txId.size == 32) { "Tron txID must be 32 bytes" }
        val contracts = transaction.getJSONObject("raw_data").getJSONArray("contract")
        require(contracts.length() > 0) { "Tron transaction has no contract" }
        val ownerAddress = contracts.getJSONObject(0)
            .getJSONObject("parameter")
            .getJSONObject("value")
            .getString("owner_address")
        val expectedHexAddress = Numeric.toHexStringNoPrefix(addressPayload(privateKey))
        require(
            ownerAddress.equals(privateKeyToAddress(privateKey), ignoreCase = true) ||
                Numeric.cleanHexPrefix(ownerAddress).equals(expectedHexAddress, ignoreCase = true)
        ) { "Tron transaction owner does not match the selected wallet" }
        val signature = Numeric.toHexStringNoPrefix(signDigest(privateKey, txId))
        val signatures = transaction.optJSONArray("signature") ?: JSONArray().also {
            transaction.put("signature", it)
        }
        if ((0 until signatures.length()).none { signatures.optString(it).equals(signature, ignoreCase = true) }) {
            signatures.put(signature)
        }
        return transaction.toString()
    }

    private fun addressPayload(privateKey: ByteArray): ByteArray {
        require(privateKey.size == 32) { "Tron private key must be 32 bytes" }
        val publicKey = Numeric.toBytesPadded(ECKeyPair.create(privateKey).publicKey, 64)
        val hash = Hash.sha3(publicKey)
        return byteArrayOf(ADDRESS_PREFIX.toByte()) + hash.copyOfRange(hash.size - 20, hash.size)
    }

    private fun signDigest(privateKey: ByteArray, digest: ByteArray): ByteArray {
        require(privateKey.size == 32) { "Tron private key must be 32 bytes" }
        require(digest.size == 32) { "Tron digest must be 32 bytes" }
        val signature = Sign.signMessage(digest, ECKeyPair.create(privateKey), false)
        return signature.r + signature.s + signature.v
    }
}
