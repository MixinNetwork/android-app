package one.mixin.android.web3

import okio.Buffer
import one.mixin.android.db.web3.vo.Web3TokenItem
import org.junit.Assert.assertThrows
import org.junit.Test
import org.sol4k.Constants
import org.sol4k.PublicKey
import org.sol4kt.CompiledAddressLookupTable
import org.sol4kt.MessageCompat
import org.sol4kt.MessageHeader
import org.sol4kt.VersionedTransactionCompat
import org.sol4kt.instruction.CompiledInstruction

class SolanaGaslessValidatorTest {
    private val sponsor = key(1)
    private val user = key(2)
    private val recipient = key(3)
    private val mint = key(4)
    private val stranger = key(5)
    private val sol = asset(one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID, 9)
    private val token = asset(mint.toBase58(), 6)

    @Test
    fun acceptsSolAndOneBoundedFee() {
        validate(transaction(solTransfer(recipient, 1_000_000_000), solTransfer(sponsor, 10_000_000)))
        validate(transaction(solTransfer(recipient, 1_000_000_000)))
    }

    @Test
    fun rejectsWrongRecipientAmountDuplicateAndUnboundedFee() {
        val transfer = solTransfer(recipient, 1_000_000_000)
        rejects(transaction(solTransfer(stranger, 1_000_000_000)))
        rejects(transaction(solTransfer(recipient, 1_000_000_001)))
        rejects(transaction(transfer, transfer))
        rejects(transaction(transfer, solTransfer(sponsor, 10_000_001)))
        rejects(transaction(transfer, solTransfer(sponsor, 10_000_000), solTransfer(sponsor, 10_000_000)))
        rejects(transaction(solTransfer(sponsor, 10_000_000)))
        rejects(transaction(transfer, solTransfer(stranger, 1)))
    }

    @Test
    fun acceptsBothTokenProgramsAndTransferEncodings() {
        for (program in listOf(Constants.TOKEN_PROGRAM_ID, Constants.TOKEN_2022_PROGRAM_ID)) {
            for (checked in listOf(false, true)) {
                validate(transaction(tokenTransfer(program, recipient, checked)), token)
            }
        }
    }

    @Test
    fun acceptsDifferentFeeAssetAndSponsorFundedAtaCreation() {
        val program = Constants.TOKEN_PROGRAM_ID
        val creation = Instruction(program = Constants.ASSOCIATED_TOKEN_PROGRAM_ID, keys = listOf(
            sponsor, ata(recipient, program), recipient, mint, Constants.SYSTEM_PROGRAM, program,
        ), data = byteArrayOf(1))
        validate(transaction(creation, tokenTransfer(program, recipient)), token)
        validate(transaction(solTransfer(recipient, 1_000_000_000), tokenTransfer(program, sponsor, amount = 10_000)), sol, token)
    }

    @Test
    fun rejectsWrongMintSourceDecimalsAuthorityAndFeeAsset() {
        val program = Constants.TOKEN_PROGRAM_ID
        val transfer = tokenTransfer(program, recipient)
        for (index in listOf(0, 1, 2, 3)) {
            rejects(transaction(transfer.copy(keys = transfer.keys.toMutableList().apply { this[index] = stranger })), token)
        }
        rejects(transaction(transfer.copy(data = transfer.data.copyOf().apply { this[9] = 9 })), token)
        rejects(transaction(transfer, solTransfer(sponsor, 10_000_000)), token)
    }

    @Test
    fun rejectsAuthorityChangesApprovalsClosuresAndUnknownPrograms() {
        val transfer = solTransfer(recipient, 1_000_000_000)
        for (program in listOf(Constants.TOKEN_PROGRAM_ID, Constants.TOKEN_2022_PROGRAM_ID)) {
            for (opcode in listOf(4, 6, 9)) {
                rejects(transaction(transfer, Instruction(program, listOf(user), byteArrayOf(opcode.toByte()))))
            }
        }
        rejects(transaction(transfer, Instruction(stranger, emptyList(), byteArrayOf())))
        rejects(transaction(transfer, Instruction(Constants.SYSTEM_PROGRAM, listOf(user), Buffer().writeIntLe(1).readByteArray())))
    }

    @Test
    fun rejectsUnexpectedAtaAndUserFundedRent() {
        val program = Constants.TOKEN_PROGRAM_ID
        val creation = Instruction(Constants.ASSOCIATED_TOKEN_PROGRAM_ID, listOf(
            sponsor, ata(recipient, program), recipient, mint, Constants.SYSTEM_PROGRAM, program,
        ), byteArrayOf(1))
        for (index in listOf(1, 2, 3, 4, 5)) {
            rejects(transaction(creation.copy(keys = creation.keys.toMutableList().apply { this[index] = stranger }), tokenTransfer(program, recipient)), token)
        }
        rejects(transaction(creation.copy(keys = creation.keys.toMutableList().apply { this[0] = user }), tokenTransfer(program, recipient)), token)
        rejects(transaction(creation.copy(data = byteArrayOf(2)), tokenTransfer(program, recipient)), token)
    }

    @Test
    fun rejectsLookupTablesInvalidIndexesAndUserFeePayer() {
        val tx = transaction(solTransfer(recipient, 1_000_000_000))
        rejects(VersionedTransactionCompat(tx.message.copy(addressLookupTables = listOf(
            CompiledAddressLookupTable(stranger, byteArrayOf(0), byteArrayOf()),
        )), mutableListOf()))
        rejects(VersionedTransactionCompat(tx.message.copy(instructions = listOf(
            tx.message.instructions.first().copy(accounts = listOf(1, 100)),
        )), mutableListOf()))
        rejects(VersionedTransactionCompat(tx.message.copy(accounts = tx.message.accounts.toMutableList().apply {
            this[0] = user
            this[1] = sponsor
        }), mutableListOf()))
    }

    @Test
    fun acceptsAccountlessComputeBudgetAndMemoButRejectsUserAccounts() {
        val transfer = solTransfer(recipient, 1_000_000_000)
        for (program in listOf(Constants.COMPUTE_BUDGET_PROGRAM_ID, PublicKey("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"))) {
            validate(transaction(transfer, Instruction(program, emptyList(), byteArrayOf(2, 0, 0, 0, 0))))
            rejects(transaction(transfer, Instruction(program, listOf(user), byteArrayOf(2, 0, 0, 0, 0))))
        }
    }

    @Test
    fun convertsDecimalsLikeExistingTransfersAndRejectsOverflow() {
        validate(transaction(solTransfer(recipient, 1_000_000_000)), amount = "1.0000000009")
        assertThrows(IllegalArgumentException::class.java) {
            validate(transaction(solTransfer(recipient, 1)), amount = "-1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            validate(transaction(solTransfer(recipient, 1)), amount = "18446744074")
        }
    }

    private fun validate(tx: VersionedTransactionCompat, asset: Web3TokenItem = sol, feeAsset: Web3TokenItem = asset, amount: String = "1") {
        tx.validateGaslessTransfer(asset, amount, user.toBase58(), recipient.toBase58(), feeAsset, "0.01")
    }

    private fun rejects(tx: VersionedTransactionCompat, asset: Web3TokenItem = sol) {
        assertThrows(IllegalArgumentException::class.java) { validate(tx, asset) }
    }

    private data class Instruction(val program: PublicKey, val keys: List<PublicKey>, val data: ByteArray)

    private fun transaction(vararg instructions: Instruction): VersionedTransactionCompat {
        val accounts = (listOf(sponsor, user) + instructions.flatMap { it.keys + it.program }).distinct()
        return VersionedTransactionCompat(MessageCompat(
            version = MessageCompat.MessageVersion.V0,
            header = MessageHeader(2, 0, 0),
            accounts = accounts,
            recentBlockhash = key(6).toBase58(),
            instructions = instructions.map {
                CompiledInstruction(it.data, it.keys.map(accounts::indexOf), accounts.indexOf(it.program))
            },
            addressLookupTables = emptyList(),
        ), mutableListOf())
    }

    private fun solTransfer(to: PublicKey, amount: Long) = Instruction(
        Constants.SYSTEM_PROGRAM, listOf(user, to), Buffer().writeIntLe(2).writeLongLe(amount).readByteArray(),
    )

    private fun tokenTransfer(program: PublicKey, to: PublicKey, checked: Boolean = true, amount: Long = 1_000_000): Instruction {
        val keys = if (checked) listOf(ata(user, program), mint, ata(to, program), user) else listOf(ata(user, program), ata(to, program), user)
        val data = Buffer().writeByte(if (checked) 12 else 3).writeLongLe(amount)
        if (checked) data.writeByte(6)
        return Instruction(program, keys, data.readByteArray())
    }

    private fun ata(owner: PublicKey, program: PublicKey) = PublicKey.findProgramDerivedAddress(owner, mint, program).publicKey

    private fun key(value: Int) = PublicKey(ByteArray(32) { value.toByte() })

    private fun asset(id: String, decimals: Int) = Web3TokenItem(
        walletId = "wallet", assetId = id, chainId = one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID,
        name = "Token", assetKey = id, symbol = "TOKEN", iconUrl = "", precision = decimals,
        balance = "10", priceUsd = "0", changeUsd = "0", chainIcon = "", chainName = "Solana",
        chainSymbol = "SOL", hidden = false, level = 0,
    )
}
