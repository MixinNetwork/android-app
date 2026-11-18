package one.mixin.android.crypto

import one.mixin.android.Constants
import one.mixin.android.db.web3.vo.isWeb3TransferSupported
import one.mixin.android.tip.bip44.Bip44Path
import org.bitcoinj.base.Base58
import org.bitcoinj.crypto.ECKey
import org.bitcoinj.crypto.MnemonicCode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.web3j.crypto.Bip32ECKeyPair.HARDENED_BIT
import org.web3j.utils.Numeric

class TronKeyGeneratorTest {
    private val mnemonic = "legal winner thank year wave sausage worth useful legal winner thank yellow"

    @Test
    fun defaultAddressMatchesTipGoReferenceVector() {
        val wallet = CryptoWalletHelper.mnemonicToTronWallet(mnemonic)

        assertEquals("TUJ2YbSDGtCqzRz7" + "quPQidRCMC98jDAPXc", wallet.address)
        assertEquals("m/44'/195'/0'/0/0", wallet.path)
        assertEquals(32, TronKeyGenerator.parsePrivateKey(wallet.privateKey).size)
        assertEquals(wallet.address, CryptoWalletHelper.privateKeyToAddress(wallet.privateKey, Constants.ChainId.TRON_CHAIN_ID))
        assertEquals(wallet.address, CryptoWalletHelper.mnemonicToAddress(mnemonic, Constants.ChainId.TRON_CHAIN_ID))
        assertTrue(TronKeyGenerator.isAddressValid(wallet.address))
    }

    @Test
    fun privateKeyOneMatchesPublicAddressVectorAndPreservesLeadingZeros() {
        val privateKey = ByteArray(32).apply { this[lastIndex] = 1 }
        val hex = Numeric.toHexStringNoPrefix(privateKey)
        val expected = "TMVQGm1qAQYVdetCeGRRkTWYYrLXuHK2HC"

        assertEquals(expected, TronKeyGenerator.privateKeyToAddress(privateKey))
        for (encoded in listOf(hex, "0x$hex", "0X${hex.uppercase()}")) {
            assertArrayEquals(privateKey, TronKeyGenerator.parsePrivateKey(encoded))
            assertEquals(expected, CryptoWalletHelper.privateKeyToAddress(encoded, Constants.ChainId.TRON_CHAIN_ID))
        }
    }

    @Test
    fun mnemonicPassphraseAndIndexArePreservedAcrossKeyAndWalletHelpers() {
        val addresses = mutableSetOf<String>()
        for (passphrase in listOf("", "TREZOR")) {
            val seed = MnemonicCode.toSeed(mnemonic.split(" "), passphrase)
            for (index in 0..9) {
                val key = TronKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
                val wallet = CryptoWalletHelper.mnemonicToTronWallet(mnemonic, passphrase, index)
                assertArrayEquals(key, TronKeyGenerator.getPrivateKeyFromSeed(seed, index))
                assertEquals(32, key.size)
                assertEquals(TronKeyGenerator.privateKeyToAddress(key), wallet.address)
                assertEquals(wallet.address, CryptoWalletHelper.mnemonicToAddress(mnemonic, Constants.ChainId.TRON_CHAIN_ID, passphrase, index))
                assertEquals(Bip44Path.tronPathString(index), wallet.path)
                assertEquals(index, CryptoWalletHelper.extractIndexFromPath(wallet.path))
                if (passphrase.isEmpty()) {
                    assertEquals(wallet.privateKey, CryptoWalletHelper.mnemonicToImportedPrivateKey(mnemonic, Constants.ChainId.TRON_CHAIN_ID, index))
                }
                addresses += wallet.address
            }
        }
        assertEquals(20, addresses.size)
        assertNotEquals(
            EthKeyGenerator.getPrivateKeyFromMnemonic(mnemonic)!!.toList(),
            TronKeyGenerator.getPrivateKeyFromMnemonic(mnemonic).toList(),
        )
    }

    @Test
    fun tronPathUsesUnhardenedAddressIndexAndRejectsOtherShapes() {
        assertArrayEquals(
            intArrayOf(44 or HARDENED_BIT, 195 or HARDENED_BIT, HARDENED_BIT, 0, 9),
            Bip44Path.tron(9),
        )
        assertThrows(IllegalArgumentException::class.java) { Bip44Path.tron(-1) }
        assertThrows(IllegalArgumentException::class.java) { Bip44Path.tronPathString(-1) }
        for (path in listOf("m/44'/195'/0'/0/-1", "m/44'/195'/0'/0/1'", "m/44'/195'/1'/0/0", "m/44'/195'/0'/1/0", "m/44'/195'/0'/0/2147483648")) {
            assertNull(CryptoWalletHelper.extractIndexFromPath(path))
        }
    }

    @Test
    fun rejectsInvalidPrivateKeysSeedsAndMnemonics() {
        val order = Numeric.toBytesPadded(ECKey.ecDomainParameters().n, 32)
        for (key in listOf(ByteArray(32), ByteArray(31) { 1 }, ByteArray(33) { 1 }, order, ByteArray(32) { 0xff.toByte() })) {
            assertThrows(IllegalArgumentException::class.java) { TronKeyGenerator.privateKeyToAddress(key) }
            assertThrows(IllegalArgumentException::class.java) { TronKeyGenerator.parsePrivateKey(Numeric.toHexString(key)) }
        }
        for (key in listOf("", "1", "0x01", "g".repeat(64), " " + "1".repeat(64))) {
            assertThrows(IllegalArgumentException::class.java) { TronKeyGenerator.parsePrivateKey(key) }
        }
        for (seed in listOf(ByteArray(15), ByteArray(65))) {
            assertThrows(IllegalArgumentException::class.java) { TronKeyGenerator.getPrivateKeyFromSeed(seed) }
        }
        assertThrows(IllegalArgumentException::class.java) { TronKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, index = -1) }
        assertThrows(org.bitcoinj.crypto.MnemonicException::class.java) {
            TronKeyGenerator.getPrivateKeyFromMnemonic(List(12) { "abandon" }.joinToString(" "))
        }
    }

    @Test
    fun validatesChecksumPrefixLengthAndCase() {
        val address = "TUJ2YbSDGtCqzRz7" + "quPQidRCMC98jDAPXc"
        assertTrue(TronKeyGenerator.isAddressValid(address))
        for (invalid in listOf(
            "", "TXYZ", " $address", address.lowercase(), address.dropLast(1) + "d",
            Base58.encodeChecked(0, ByteArray(20)),
            Base58.encodeChecked(0x41, ByteArray(19)),
            Base58.encodeChecked(0x41, ByteArray(21)),
            "41" + "00".repeat(20),
        )) {
            assertFalse(invalid, TronKeyGenerator.isAddressValid(invalid))
        }
    }

    @Test
    fun keySupportDoesNotEnableTransfersOrClassifyTronAsEvmOrUtxo() {
        assertFalse(isWeb3TransferSupported(Constants.ChainId.TRON_CHAIN_ID))
        assertFalse(Constants.ChainId.TRON_CHAIN_ID in Constants.Web3ChainIds)
        assertFalse(Constants.ChainId.TRON_CHAIN_ID in Constants.Web3EvmChainIds)
        assertFalse(Constants.ChainId.TRON_CHAIN_ID in Constants.Web3UtxoChainIds)
    }
}
