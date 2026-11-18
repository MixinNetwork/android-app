package one.mixin.android.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import blockchain.Blockchain
import one.mixin.android.Constants
import one.mixin.android.extension.hexString
import one.mixin.android.tip.bip44.Bip44Path
import org.bitcoinj.base.Base58
import org.bitcoinj.crypto.MnemonicCode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class TronGoParityTest {
    @Test
    fun mnemonicKeysAndAddressesMatchTipGoAcrossIndexesAndPassphrases() {
        val mnemonic = "legal winner thank year wave sausage worth useful legal winner thank yellow"
        val addresses = mutableSetOf<String>()
        for (passphrase in listOf("", "TREZOR")) {
            val seed = MnemonicCode.toSeed(mnemonic.split(" "), passphrase)
            for (index in 0..9) {
                val wallet = CryptoWalletHelper.mnemonicToTronWallet(mnemonic, passphrase, index)
                val derived = TipKeyGenerator.generate(seed, Constants.ChainId.TRON_CHAIN_ID, index)
                val path = Bip44Path.tronPathString(index)
                val expected = Blockchain.generateTronAddress(seed.hexString(), path)

                assertEquals(expected, wallet.address)
                assertEquals(expected, derived.address)
                assertEquals(expected, CryptoWalletHelper.privateKeyToAddress(wallet.privateKey, Constants.ChainId.TRON_CHAIN_ID))
                assertEquals(expected, Blockchain.generateTronAddressFromPrivateKey(wallet.privateKey))
                assertEquals(wallet.privateKey, Blockchain.exportTronPrivateKeyFromSeed(seed.hexString(), path))
                assertArrayEquals(derived.privateKey, TronKeyGenerator.parsePrivateKey(wallet.privateKey))
                if (passphrase.isEmpty()) {
                    assertEquals(expected, Blockchain.generateTronAddressFromMnemonic(mnemonic, path))
                    assertEquals(wallet.privateKey, Blockchain.exportTronPrivateKey(mnemonic, path))
                    assertEquals(wallet.privateKey, CryptoWalletHelper.mnemonicToImportedPrivateKey(mnemonic, Constants.ChainId.TRON_CHAIN_ID, index))
                }
                addresses += expected
            }
        }
        assertEquals(20, addresses.size)
        val seed = MnemonicCode.toSeed(mnemonic.split(" "), "")
        assertEquals(
            Blockchain.generateTronAddress(seed.hexString(), ""),
            Blockchain.generateTronAddress(seed.hexString(), Bip44Path.tronPathString()),
        )
        assertEquals(
            CryptoWalletHelper.mnemonicToTronWallet(mnemonic).privateKey,
            Blockchain.exportTronPrivateKey(mnemonic, ""),
        )
    }

    @Test
    fun tipSeedsMatchGoWithoutMnemonicConversion() {
        for (seed in listOf(ByteArray(32) { it.toByte() }, ByteArray(32) { (255 - it).toByte() })) {
            val addresses = mutableSetOf<String>()
            for (index in 0..9) {
                val derived = TipKeyGenerator.generate(seed, Constants.ChainId.TRON_CHAIN_ID, index)
                val path = Bip44Path.tronPathString(index)
                val expected = Blockchain.generateTronAddress(seed.hexString(), path)

                assertEquals(expected, derived.address)
                assertEquals(expected, TronKeyGenerator.privateKeyToAddress(derived.privateKey))
                assertEquals(Numeric.toHexString(derived.privateKey), Blockchain.exportTronPrivateKeyFromSeed(seed.hexString(), path))
                assertEquals(expected, Blockchain.generateTronAddressFromPrivateKey(derived.privateKey.hexString()))
                assertArrayEquals(TronKeyGenerator.getPrivateKeyFromSeed(seed, index), derived.privateKey)
                addresses += expected
            }
            assertEquals(10, addresses.size)
        }
    }

    @Test
    fun addressValidationAndEncodingMatchGo() {
        val privateKey = ByteArray(32).apply { this[lastIndex] = 1 }
        val address = TronKeyGenerator.privateKeyToAddress(privateKey)
        for (encoded in listOf(privateKey.hexString(), Numeric.toHexString(privateKey), "0X${privateKey.hexString()}")) {
            assertEquals(address, Blockchain.generateTronAddressFromPrivateKey(encoded))
        }
        assertTrue(Blockchain.isTronAddressValid(address))
        val hexAddress = Base58.decodeChecked(address).hexString()
        assertEquals(hexAddress, Blockchain.tronAddressToHex(address))
        assertEquals(address, Blockchain.tronAddressFromHex(hexAddress))
        assertEquals(address, Blockchain.tronAddressFromHex("0X${hexAddress.uppercase()}"))
        for (invalid in listOf(
            "", " $address", address.lowercase(), address.uppercase(), hexAddress,
            Base58.encodeChecked(0, ByteArray(20)),
            Base58.encodeChecked(0x41, ByteArray(19)),
            Base58.encodeChecked(0x41, ByteArray(21)),
        )) {
            assertFalse(TronKeyGenerator.isAddressValid(invalid))
            assertEquals(TronKeyGenerator.isAddressValid(invalid), Blockchain.isTronAddressValid(invalid))
        }
    }

    @Test
    fun messageV2SignaturesMatchAndroidCrypto() {
        val privateKey = ByteArray(32).apply { this[lastIndex] = 1 }
        val keyPair = ECKeyPair.create(privateKey)
        val address = TronKeyGenerator.privateKeyToAddress(privateKey)
        for (message in listOf("hello world", "", "汉💡", "0x1234")) {
            val bytes = message.toByteArray(Charsets.UTF_8)
            val prefix = "\u0019TRON Signed Message:\n${bytes.size}".toByteArray(Charsets.UTF_8)
            val digest = Hash.sha3(prefix + bytes)
            val signature = Sign.signMessage(digest, keyPair, false)
            val expected = Numeric.toHexString(signature.r + signature.s + signature.v)

            assertEquals(expected, Blockchain.signTronMessageV2(privateKey.hexString(), message))
            assertEquals(address, Blockchain.recoverTronMessageV2Address(message, expected))
        }
    }

    @Test
    fun rawDataHashSignaturesAndTrc20EncodingMatchAndroidCrypto() {
        val privateKey = ByteArray(32).apply { this[lastIndex] = 1 }
        val keyPair = ECKeyPair.create(privateKey)
        val address = TronKeyGenerator.privateKeyToAddress(privateKey)
        val rawData = byteArrayOf(0, 1, 2, 3)
        val digest = MessageDigest.getInstance("SHA-256").digest(rawData)
        val signature = Sign.signMessage(digest, keyPair, false)
        assertEquals(digest.hexString(), Blockchain.tronTransactionID(rawData.hexString()))
        assertEquals((signature.r + signature.s + signature.v).hexString(), Blockchain.signTronRawTransaction(privateKey.hexString(), rawData.hexString()))

        val addressWord = Base58.decodeChecked(address).copyOfRange(1, 21).hexString().padStart(64, '0')
        for (amount in listOf(BigInteger.ZERO, BigInteger.valueOf(1_000_000), BigInteger.ONE.shiftLeft(256) - BigInteger.ONE)) {
            val expected = "a9059cbb" + addressWord + amount.toString(16).padStart(64, '0')
            assertEquals(expected, Blockchain.encodeTrc20TransferData(address, amount.toString()))
        }
    }
}
