package one.mixin.android

import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.bip44.generateBip44Key
import org.junit.Test
import org.sol4k.Connection
import org.sol4k.Keypair
import org.sol4k.PublicKey
import org.sol4k.RpcUrl
import org.sol4k.Transaction
import org.sol4k.instruction.TransferInstruction
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Bip44Test {
    @Test
    fun testBip44Ethereum() {
        val seed = "f01a27c0cafc921b3a1e1e4bd5c8cc9e1fe8e7cf2edcd9a846233d1e55462768"
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed.hexStringToByteArray())
        val key = generateBip44Key(masterKeyPair, Bip44Path.Ethereum)
        val address = Keys.toChecksumAddress(Keys.getAddress(key.publicKey))

        assertContentEquals(key.publicKey.toByteArray(), "6defa9e4188f3ca577c40895358391ec58c54f7455d5d83b8c883f018a8e01fe7ab418019f5e9bd7de0a43dd08ee68f60a8ed1d5df62aed19d99f4a187d85f4e".hexStringToByteArray())
        assertEquals(address, "0x28fB45dbcb4d244Fed7e824F1fb1f19DCd283D06")
    }

    @Test
    fun testBip44Solana() {
        val priv = "f01a27c0cafc921b3a1e1e4bd5c8cc9e1fe8e7cf2edcd9a846233d1e55462768".hexStringToByteArray()
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(priv)
        val bip44KeyPair = generateBip44Key(masterKeyPair, Bip44Path.Solana)
        val seed = Numeric.toBytesPadded(bip44KeyPair.privateKey, 32)
        val kp = Keypair.fromSecretKey(seed)
        assertEquals("CNH3eKGGKVTP8PiZyZdgc4Pc9jshFzr3bR1u1RtCCmEK", kp.publicKey.toBase58())
        getWalletBalance(kp.publicKey.toBase58())
    }

    @Test
    fun testBip44Solana1() {
        val priv = "f01a27c0cafc921b3a1e1e4bd5c8cc9e1fe8e7cf2edcd9a846233d1e55462768".hexStringToByteArray()
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(priv)
        val bip44KeyPair = generateBip44Key(masterKeyPair, Bip44Path.Solana)
        val seed = Numeric.toBytesPadded(bip44KeyPair.privateKey, 32)
        val kp = Keypair.fromSecretKey(seed)
        assertEquals("CNH3eKGGKVTP8PiZyZdgc4Pc9jshFzr3bR1u1RtCCmEK", kp.publicKey.toBase58())
        getWalletBalance(kp.publicKey.toBase58())
    }

    private fun getWalletBalance(address: String) {
        val connection = Connection(RpcUrl.DEVNET)
        val wallet = PublicKey(address)
        val balance = connection.getBalance(wallet)
        println("Balance in Lamports: $balance")
    }

    @Test
    fun tesSolTransfer() {
        val priv = "f01a27c0cafc921b3a1e1e4bd5c8cc9e1fe8e7cf2edcd9a846233d1e55462768".hexStringToByteArray()
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(priv)
        val bip44KeyPair = generateBip44Key(masterKeyPair, Bip44Path.Solana)
        val seed = Numeric.toBytesPadded(bip44KeyPair.privateKey, 32)
        val sender = Keypair.fromSecretKey(seed)

        val connection = Connection(RpcUrl.DEVNET)
        val blockhash: String = connection.getLatestBlockhash()

        val receiver = PublicKey("9B5XszUGdMaxCZ7uSQhPzdks5ZQSmWxrmzCSvtJ6Ns6g")
        val instruction = TransferInstruction(sender.publicKey, receiver, 100L)
        val transaction = Transaction(
            blockhash,
            instruction,
            sender.publicKey
        )
        transaction.sign(sender)
        val signature: String = connection.sendTransaction(transaction.serialize())
        println("Transaction Signature: $signature\ntx: ${transaction.serialize().base64Encode()}")
    }
}
