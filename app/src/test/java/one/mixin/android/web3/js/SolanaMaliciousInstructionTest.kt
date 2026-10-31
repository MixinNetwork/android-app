package one.mixin.android.web3.js

import one.mixin.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.sol4k.Constants
import org.sol4k.PublicKey
import org.sol4kt.MessageCompat
import org.sol4kt.MessageHeader
import org.sol4kt.VersionedTransactionCompat
import org.sol4kt.instruction.CompiledInstruction

class SolanaMaliciousInstructionTest {
    private val user = PublicKey(ByteArray(32) { 1 })
    private val other = PublicKey(ByteArray(32) { 2 })
    private val tokenPrograms = listOf(Constants.TOKEN_PROGRAM_ID, Constants.TOKEN_2022_PROGRAM_ID)

    @Test
    fun blocksTokenAuthorityAndApprovalsOnlyForUser() {
        for (version in MessageCompat.MessageVersion.entries) {
            for (program in tokenPrograms) {
                for ((opcode, ownerIndex) in listOf(6 to 1, 4 to 2, 13 to 3)) {
                    val error = if (opcode == 6) R.string.malicious_instruction_token_set_authority else R.string.malicious_instruction_token_approve
                    val accounts = MutableList(ownerIndex + 1) { 1 }
                    accounts[ownerIndex] = 0
                    assertEquals(error, transaction(program, byteArrayOf(opcode.toByte()), accounts, version).maliciousInstructionError(user.toBase58()))
                    accounts[ownerIndex] = 1
                    accounts[0] = 0
                    assertNull(transaction(program, byteArrayOf(opcode.toByte()), accounts, version).maliciousInstructionError(user.toBase58()))
                }
            }
        }
    }

    @Test
    fun blocksSystemAssignmentsOnlyForAssignedUser() {
        for (opcode in listOf(1, 10)) {
            val data = byteArrayOf(opcode.toByte(), 0, 0, 0)
            assertEquals(R.string.malicious_instruction_system_assign, transaction(Constants.SYSTEM_PROGRAM, data, listOf(0, 1)).maliciousInstructionError(user.toBase58()))
            assertNull(transaction(Constants.SYSTEM_PROGRAM, data, listOf(1, 0)).maliciousInstructionError(user.toBase58()))
        }
    }

    @Test
    fun ignoresMissingAndNonStaticAccounts() {
        for ((program, data, ownerIndex) in listOf(
            Triple(Constants.TOKEN_PROGRAM_ID, byteArrayOf(6), 1),
            Triple(Constants.TOKEN_2022_PROGRAM_ID, byteArrayOf(4), 2),
            Triple(Constants.TOKEN_PROGRAM_ID, byteArrayOf(13), 3),
            Triple(Constants.SYSTEM_PROGRAM, byteArrayOf(1, 0, 0, 0), 0),
            Triple(Constants.SYSTEM_PROGRAM, byteArrayOf(10, 0, 0, 0), 0),
        )) {
            assertNull(transaction(program, data, emptyList()).maliciousInstructionError(user.toBase58()))
            for (index in listOf(-1, 3, 255)) {
                val accounts = MutableList(ownerIndex + 1) { 0 }
                accounts[ownerIndex] = index
                assertNull(transaction(program, data, accounts).maliciousInstructionError(user.toBase58()))
            }
        }
        val tx = transaction(Constants.TOKEN_PROGRAM_ID, byteArrayOf(6), listOf(1, 0))
        for (index in listOf(-1, 3, 255)) {
            tx.message.instructions = listOf(tx.message.instructions.single().copy(programIdIndex = index))
            assertNull(tx.maliciousInstructionError(user.toBase58()))
        }
    }

    @Test
    fun permitsTransfersOtherProgramsAndUnrecognizedData() {
        for (program in tokenPrograms) {
            for (data in listOf(byteArrayOf(), byteArrayOf(3), byteArrayOf(12), byteArrayOf(9))) {
                assertNull(transaction(program, data, listOf(0, 0, 0, 0)).maliciousInstructionError(user.toBase58()))
            }
        }
        for (data in listOf(byteArrayOf(), byteArrayOf(1), byteArrayOf(10, 0, 0), byteArrayOf(2, 0, 0, 0), byteArrayOf(1, 1, 0, 0))) {
            assertNull(transaction(Constants.SYSTEM_PROGRAM, data, listOf(0)).maliciousInstructionError(user.toBase58()))
        }
        assertNull(transaction(other, byteArrayOf(6), listOf(1, 0)).maliciousInstructionError(user.toBase58()))
    }

    @Test
    fun checksInstructionsAfterLegitimateTransfer() {
        val tx = transaction(Constants.TOKEN_PROGRAM_ID, byteArrayOf(6), listOf(1, 0))
        tx.message.instructions = listOf(
            CompiledInstruction(byteArrayOf(3), listOf(1, 1, 0), 2),
            tx.message.instructions.single(),
        )
        assertEquals(R.string.malicious_instruction_token_set_authority, tx.maliciousInstructionError(user.toBase58()))
    }

    private fun transaction(
        program: PublicKey,
        data: ByteArray,
        accounts: List<Int>,
        version: MessageCompat.MessageVersion = MessageCompat.MessageVersion.V0,
    ) = VersionedTransactionCompat(
        message = MessageCompat(
            version = version,
            header = MessageHeader(1, 0, 1),
            accounts = listOf(user, other, program),
            recentBlockhash = user.toBase58(),
            instructions = listOf(CompiledInstruction(data, accounts, 2)),
            addressLookupTables = emptyList(),
        ),
        signatures = mutableListOf(),
    )
}
