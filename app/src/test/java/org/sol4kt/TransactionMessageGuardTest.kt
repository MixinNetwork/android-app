package org.sol4kt

import one.mixin.android.extension.base64Encode
import org.junit.Assert.assertThrows
import org.junit.Test
import org.sol4k.PublicKey
import org.sol4k.Transaction
import org.sol4k.instruction.TransferInstruction
import org.sol4kt.exception.MaliciousInstructionException

class TransactionMessageGuardTest {
    private val payer = PublicKey("5TDMKU3basuWC9sb9xAJgvn17KYFTLk9srPifmjZqJH9")
    private val receiver = PublicKey("9B5XszUGdMaxCZ7uSQhPzdks5ZQSmWxrmzCSvtJ6Ns6g")

    private fun transferMessage(): ByteArray {
        val transaction = Transaction(payer.toBase58(), TransferInstruction(payer, receiver, 100L), payer)
        transaction.addPlaceholderSignature()
        return VersionedTransactionCompat.from(transaction.serialize().base64Encode()).message.serialize()
    }

    @Test
    fun rejectsTransactionMessageForSigner() {
        val message = transferMessage()
        assertThrows(MaliciousInstructionException::class.java) {
            requireNotTransactionMessage(message, payer)
        }
    }

    @Test
    fun allowsTransactionMessageForNonSigner() {
        requireNotTransactionMessage(transferMessage(), receiver)
    }

    @Test
    fun allowsPlainText() {
        requireNotTransactionMessage("Sign in to example.com\nNonce: 12345".toByteArray(), payer)
        requireNotTransactionMessage(ByteArray(0), payer)
    }
}
