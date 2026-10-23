package one.mixin.android.crypto

import one.mixin.android.Constants
import one.mixin.android.extension.hexStringToByteArray
import org.bitcoinj.base.Bech32
import org.bitcoinj.base.ScriptType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UtxoKeyGeneratorTest {
    private val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

    @Test
    fun derivesLitecoinBip84PublicReferenceVectors() {
        val vectors = listOf(
            LitecoinVector("", 0, "4ab54480cbbaa53d5d3ba43a3e85d221df085490e88346ad11579e17000db7bb", "ltc1qjmxnz78nmc8nq77wuxh25n2es7rzm5c2rkk4wh"),
            LitecoinVector("", 1, "61fa14b9b3d872aaa327855cc94b5a220094395d55d01319cfee32e236a26d1e", "ltc1qwlezpr3890hcp6vva9twqh27mr6edadreqvhnn"),
            LitecoinVector("TREZOR", 0, "bcfe1b0501e08cc121a9d44ab76f70de210d89a394c9014ba933404888178308", "ltc1qmnmdtk82gfvv4khc3w9lrugre34cj65kamf9tz"),
            LitecoinVector("TREZOR", 1, "ead797a6c88b7fc63fcfec5498a363ea878ab8f64d328927a34d091e01abcd20", "ltc1qxwc6uc7q24vu8njkkgk3a3uzq4xwvak54y8k53"),
        )

        vectors.forEach { vector ->
            val privateKey = UtxoKeyGenerator.getPrivateKeyFromMnemonic(
                mnemonic,
                Constants.ChainId.Litecoin,
                vector.passphrase,
                vector.index,
            )
            assertEquals(vector.privateKeyHex, privateKey.joinToString("") { "%02x".format(it) })
            assertEquals(vector.address, UtxoKeyGenerator.privateKeyToAddress(privateKey, Constants.ChainId.Litecoin))
            assertEquals(vector.address, UtxoKeyGenerator.mnemonicToAddress(mnemonic, Constants.ChainId.Litecoin, vector.passphrase, vector.index))
        }
    }

    @Test
    fun litecoinSeedAndMnemonicKeysAgreeWithReferenceAtNonzeroIndex() {
        val seed = org.bitcoinj.crypto.MnemonicCode.toSeed(mnemonic.split(" "), "TREZOR")
        val privateKey = UtxoKeyGenerator.getPrivateKeyFromSeed(seed, Constants.ChainId.Litecoin, 1)
        val address = UtxoKeyGenerator.privateKeyToAddress(privateKey, Constants.ChainId.Litecoin)
        assertEquals("ltc1qxwc6uc7q24vu8njkkgk3a3uzq4xwvak54y8k53", address)
        assertEquals("ead797a6c88b7fc63fcfec5498a363ea878ab8f64d328927a34d091e01abcd20", privateKey.joinToString("") { "%02x".format(it) })
        val wallet = CryptoWalletHelper.mnemonicToLitecoinWallet(mnemonic, "TREZOR", 1)
        assertEquals(address, wallet.address)
        assertEquals("m/84'/2'/0'/0/1", wallet.path)
    }

    @Test
    fun derivesLitecoinAddressesForTenIndexesMatchingTipGo() {
        val mnemonic = "legal winner thank year wave sausage worth useful legal winner thank yellow"
        val expected = listOf(
            "ltc1qg0457yp54vvw74mdkrqhkvp2fyu387x2c8zs43",
            "ltc1qyphatmk7eyt42trujmw7xv6hm6xtn02nrwz3dc",
            "ltc1q2pukppfle0lfshrgvk5p9f3u3egj3zm5umndjl",
            "ltc1qlnjvu55vv4ha38xxlh3hphfl7wuz0y5cnvp2ec",
            "ltc1qngdv4kjwvzcc2elvch2rhzphs2m2knlhs02999",
            "ltc1q9rypjupr6p07lw5me23ad3t66ez3at2l4w4skc",
            "ltc1qfwkceza9zaeqcrt2dje00e9eckdwjw08v8w49a",
            "ltc1qyv0kumrp25pjng3n3xjepxvftkj83ykgt2pg8w",
            "ltc1qls842u5p03n8w9e5xvl5tw9sfaaq5kz8cy2k23",
            "ltc1qa6fngmkg45qgyhqjw8za4ug8ep33uy9ylwf52m",
        )
        val seed = org.bitcoinj.crypto.MnemonicCode.toSeed(mnemonic.split(" "), "")

        expected.forEachIndexed { index, address ->
            val privateKey = UtxoKeyGenerator.getPrivateKeyFromSeed(seed, Constants.ChainId.Litecoin, index)
            assertEquals(address, UtxoKeyGenerator.privateKeyToAddress(privateKey, Constants.ChainId.Litecoin))
            assertEquals(address, UtxoKeyGenerator.mnemonicToAddress(mnemonic, Constants.ChainId.Litecoin, "", index))
            val wallet = CryptoWalletHelper.mnemonicToLitecoinWallet(mnemonic, "", index)
            assertEquals(address, wallet.address)
            assertEquals("m/84'/2'/0'/0/$index", wallet.path)
        }
    }

    @Test
    fun importedLitecoinHexAndWifResolveToTheSameSigningKey() {
        val hex = "4ab54480cbbaa53d5d3ba43a3e85d221df085490e88346ad11579e17000db7bb"
        val expected = hex.hexStringToByteArray()
        listOf(hex, "0x$hex", "0X$hex", "T5ZCYhLqXu6EJKk2nhjvwsaLH357CisixhLGWpKXEiqWTUtzte6o").forEach {
            assertTrue(expected.contentEquals(UtxoKeyGenerator.parseLitecoinPrivateKey(it)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            UtxoKeyGenerator.parseLitecoinPrivateKey("0x02")
        }
    }

    @Test
    fun litecoinWifUsesPrefix176AndRoundTripsCompressedKey() {
        val privateKey = "4ab54480cbbaa53d5d3ba43a3e85d221df085490e88346ad11579e17000db7bb".hexStringToByteArray()
        val wif = UtxoChainConfig.forChain(Constants.ChainId.Litecoin).wif.encode(privateKey)

        assertEquals("T5ZCYhLqXu6EJKk2nhjvwsaLH357CisixhLGWpKXEiqWTUtzte6o", wif)
        assertTrue(privateKey.contentEquals(UtxoChainConfig.forChain(Constants.ChainId.Litecoin).wif.decode(wif)))
        assertEquals(
            "ltc1qjmxnz78nmc8nq77wuxh25n2es7rzm5c2rkk4wh",
            UtxoKeyGenerator.privateKeyToAddress(wif, Constants.ChainId.Litecoin),
        )
        assertThrows(IllegalArgumentException::class.java) {
            UtxoKeyGenerator.privateKeyToAddress(
                "Kyiw6x3f8X7dXV7AF4o4jX2xLBRo8drq9VS1f1gyfkfLwbPDfHBe",
                Constants.ChainId.Litecoin,
            )
        }
    }

    @Test
    fun litecoinPrivateKeyInputsRejectMalformedAndInvalidScalars() {
        listOf(
            "0x02",
            "f".repeat(64),
            "z".repeat(64),
        ).forEach { invalidKey ->
            assertThrows(IllegalArgumentException::class.java) {
                UtxoKeyGenerator.privateKeyToAddress(invalidKey, Constants.ChainId.Litecoin)
            }
        }
        listOf(
            ByteArray(32),
            ByteArray(32) { 0xff.toByte() },
            ByteArray(31) { 1 },
        ).forEach { invalidKey ->
            assertThrows(IllegalArgumentException::class.java) {
                UtxoKeyGenerator.privateKeyToAddress(invalidKey, Constants.ChainId.Litecoin)
            }
        }
    }

    @Test
    fun litecoinParserAcceptsOnlyMainnetTransparentFormats() {
        val accepted = listOf(
            "ltc1qjmxnz78nmc8nq77wuxh25n2es7rzm5c2rkk4wh" to ScriptType.P2WPKH,
            "LYyKS4hm5QcmAWntuZSA6Kp5TKg9fCeLQn" to ScriptType.P2PKH,
            "MUi6eFEWq7Sj3XaWUzJTDvSFTpaSdDR3fq" to ScriptType.P2SH,
            "3NVxLMpYszbJF2JcP7K7QHBr97yzhfQbf4" to ScriptType.P2SH,
        )
        accepted.forEach { (address, type) ->
            assertEquals(type, UtxoKeyGenerator.parseAddress(address, Constants.ChainId.Litecoin).outputScriptType)
        }

        listOf(
            "bc1qjmxnz78nmc8nq77wuxh22n2es7rzm5c29t0889",
            "ltc1qjmxnz78nmc8nq77wuxh25n2es7rzm5c2rkk4wx",
            "ltc1mweb1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq",
            "bc1p5cyxnuxmeuwuvkwfem96lxxss9j4e3h3q8z4j0",
        ).forEach { address ->
            assertFalse(UtxoKeyGenerator.isAddressValid(address, Constants.ChainId.Litecoin))
        }
    }

    @Test
    fun litecoinCorePolicyConstantsAreExplicit() {
        val config = UtxoChainConfig.forChain(Constants.ChainId.Litecoin)

        assertEquals(8, config.decimals)
        assertEquals(48, config.network.legacyAddressHeader())
        assertEquals(50, config.network.legacyP2SHHeader())
        assertEquals("ltc", config.network.segwitAddressHrp())
        assertEquals(176, config.wif.prefix)
        assertEquals(30_000L, config.dustRelayFeeAtomicPerKvB)
        assertEquals(1_000L, config.incrementalRelayFeeAtomicPerKvB)
        assertEquals(10_000L, config.walletMinimumFeeAtomicPerKvB)
        assertEquals(2_940L, config.dustThresholdAtomic(ScriptType.P2WPKH))
        assertEquals(5_400L, config.dustThresholdAtomic(ScriptType.P2SH))
        assertEquals(5_460L, config.dustThresholdAtomic(ScriptType.P2PKH))
    }

    @Test
    fun litecoinDerivationPathAndIndexAreExact() {
        val config = UtxoChainConfig.forChain(Constants.ChainId.Litecoin)

        assertEquals("m/84'/2'/0'/0/7", config.derivationPathString(7))
        assertEquals(7, config.extractAddressIndex("m/84'/2'/0'/0/7"))
        assertEquals(null, config.extractAddressIndex("m/84'/2'/1'/0/7"))
        assertEquals(null, config.extractAddressIndex("m/84'/2'/0'/1/7"))
        assertEquals(null, config.extractAddressIndex("m/84'/2'/0'/0/-1"))
        assertEquals(null, config.extractAddressIndex("m/84'/2'/0'/0/07"))
    }

    @Test
    fun derivesDistinctPearlAddressesForSeedIndexes() {
        val seed = ByteArray(32) { index -> (index + 1).toByte() }
        val firstAddress =
            UtxoKeyGenerator.privateKeyToAddress(
                UtxoKeyGenerator.getPrivateKeyFromSeed(
                    seed,
                    Constants.ChainId.PEARL_CHAIN_ID,
                    index = 1,
                ),
                Constants.ChainId.PEARL_CHAIN_ID,
            )
        val secondAddress =
            UtxoKeyGenerator.privateKeyToAddress(
                UtxoKeyGenerator.getPrivateKeyFromSeed(
                    seed,
                    Constants.ChainId.PEARL_CHAIN_ID,
                    index = 2,
                ),
                Constants.ChainId.PEARL_CHAIN_ID,
            )

        assertNotEquals(firstAddress, secondAddress)
    }

    @Test
    fun generatesBitcoinAndPearlAddressesWithOneGenerator() {
        val seed = ByteArray(32) { index -> (index + 1).toByte() }
        val bitcoinPrivateKey = UtxoKeyGenerator.getPrivateKeyFromSeed(
            seed,
            Constants.ChainId.BITCOIN_CHAIN_ID,
        )
        val pearlPrivateKey = UtxoKeyGenerator.getPrivateKeyFromSeed(
            seed,
            Constants.ChainId.PEARL_CHAIN_ID,
        )
        val bitcoinAddress = UtxoKeyGenerator.privateKeyToAddress(
            bitcoinPrivateKey,
            Constants.ChainId.BITCOIN_CHAIN_ID,
        )
        val pearlAddress = UtxoKeyGenerator.privateKeyToAddress(
            pearlPrivateKey,
            Constants.ChainId.PEARL_CHAIN_ID,
        )

        assertEquals(32, bitcoinPrivateKey.size)
        assertEquals(32, pearlPrivateKey.size)
        assertTrue(bitcoinAddress.startsWith("bc1"))
        assertTrue(pearlAddress.startsWith("prl1"))
        assertTrue(UtxoKeyGenerator.isAddressValid(bitcoinAddress, Constants.ChainId.BITCOIN_CHAIN_ID))
        assertTrue(UtxoKeyGenerator.isAddressValid(pearlAddress, Constants.ChainId.PEARL_CHAIN_ID))
        assertNotEquals(bitcoinAddress, pearlAddress)
    }

    @Test
    fun usesStandardBip32ForPearlHardenedDerivation() {
        val seed = (
            "1d00304f6e8daccbea0e2d4c6b8aa9c8e70b2a496887a6c5e40827466584a3c2" +
                "e10524436281a0bfde0221405f7e9dbcdbfa1e3d5c7b9ab9d8f71b3a597897b6"
        ).hexStringToByteArray()
        val privateKey = UtxoKeyGenerator.getPrivateKeyFromSeed(
            seed,
            Constants.ChainId.PEARL_CHAIN_ID,
        )

        assertEquals(
            "prl1p54m4h2agaw92ce58havk6n9l82qgnp8nyyfhad3ycmddsym2rqwqhs07z6",
            UtxoKeyGenerator.privateKeyToAddress(privateKey, Constants.ChainId.PEARL_CHAIN_ID),
        )
    }

    @Test
    fun rejectsTaprootAddressWithNonZeroPadding() {
        val validAddress = "prl1p5rg2k5twnlggzdqhcw994xkgwqfuvvwhjnjrx84xsv9f834r887q4j2j5p"
        assertTrue(UtxoKeyGenerator.isAddressValid(validAddress, Constants.ChainId.PEARL_CHAIN_ID))
        val words = Bech32.decode(validAddress).bytes()
        words[words.lastIndex] = (words.last().toInt() or 1).toByte()
        val invalidAddress = Bech32.encode(
            Bech32.Encoding.BECH32M,
            "prl",
            object : Bech32.Bech32Bytes(words) {},
        )

        assertFalse(UtxoKeyGenerator.isAddressValid(invalidAddress, Constants.ChainId.PEARL_CHAIN_ID))
    }

    private data class LitecoinVector(
        val passphrase: String,
        val index: Int,
        val privateKeyHex: String,
        val address: String,
    )
}
