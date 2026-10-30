package one.mixin.android.web3

import okio.Buffer
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.web3.js.throwIfAnyMaliciousInstruction
import org.sol4k.Constants
import org.sol4k.PublicKey
import org.sol4kt.VersionedTransactionCompat
import java.math.BigDecimal

fun VersionedTransactionCompat.validateGaslessTransfer(
    token: Web3TokenItem,
    amount: String,
    fromAddress: String,
    toAddress: String,
    feeToken: Web3TokenItem,
    feeAmount: String,
) {
    val user = PublicKey(fromAddress)
    val recipient = PublicKey(toAddress)
    val accounts = message.accounts
    require(message.addressLookupTables.isEmpty())
    require(accounts.distinct().size == accounts.size)
    require(message.header.numRequireSignatures in 2..accounts.size)
    require(message.header.numReadonlySignedAccounts in 0 until message.header.numRequireSignatures)
    require(message.header.numReadonlyUnsignedAccounts in 0..(accounts.size - message.header.numRequireSignatures))
    val sponsor = accounts.first()
    require(sponsor != user && user in accounts.take(message.header.numRequireSignatures))
    require(token.chainId == one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID && feeToken.chainId == token.chainId)

    fun units(value: String, asset: Web3TokenItem): Long {
        require(asset.precision in 0..255)
        val decimal = BigDecimal(value)
        require(decimal.signum() >= 0)
        val units = decimal.multiply(BigDecimal.TEN.pow(if (asset.isNativeSolAsset()) 9 else asset.precision)).toBigInteger()
        require(units.bitLength() <= 63)
        return units.toLong()
    }

    val transferUnits = units(amount, token)
    val feeUnits = units(feeAmount, feeToken)
    require(transferUnits > 0)
    var transferred = false
    var paidFee = false

    fun ata(owner: PublicKey, asset: Web3TokenItem, program: PublicKey): PublicKey =
        PublicKey.findProgramDerivedAddress(owner, PublicKey(asset.assetKey), program).publicKey

    for (instruction in message.instructions) {
        require(instruction.programIdIndex in accounts.indices)
        require(instruction.accounts.all { it in accounts.indices })
        val program = accounts[instruction.programIdIndex]
        val keys = instruction.accounts.map { accounts[it] }
        val data = instruction.data
        when (program) {
            Constants.SYSTEM_PROGRAM, Constants.TOKEN_PROGRAM_ID, Constants.TOKEN_2022_PROGRAM_ID -> {
                fun matches(asset: Web3TokenItem, destination: PublicKey, expected: Long): Boolean {
                    if (program == Constants.SYSTEM_PROGRAM) {
                        if (!asset.isNativeSolAsset() || keys != listOf(user, destination) || data.size != 12) return false
                        val buffer = Buffer().write(data)
                        return buffer.readIntLe() == 2 && buffer.readLongLe() == expected
                    }
                    if (asset.isNativeSolAsset() || data.isEmpty()) return false
                    val checked = data[0].toInt() == 12
                    if (!checked && data[0].toInt() != 3) return false
                    if (data.size != (if (checked) 10 else 9)) return false
                    val target = if (checked) keys.getOrNull(2) else keys.getOrNull(1)
                    if (target != destination && target != ata(destination, asset, program)) return false
                    val expectedKeys = if (checked) {
                        listOf(ata(user, asset, program), PublicKey(asset.assetKey), target, user)
                    } else {
                        listOf(ata(user, asset, program), target, user)
                    }
                    if (keys != expectedKeys) return false
                    val buffer = Buffer().write(data)
                    buffer.readByte()
                    return buffer.readLongLe() == expected && (!checked || buffer.readByte().toUByte().toInt() == asset.precision)
                }
                when {
                    !transferred && matches(token, recipient, transferUnits) -> transferred = true
                    !paidFee && feeUnits > 0 && matches(feeToken, sponsor, feeUnits) -> paidFee = true
                    else -> throw IllegalArgumentException()
                }
            }
            Constants.ASSOCIATED_TOKEN_PROGRAM_ID -> {
                require(data.isEmpty() || (data.size == 1 && data[0].toInt() in 0..1))
                require(keys.size in 6..7)
                require(keys[0] == sponsor && keys[4] == Constants.SYSTEM_PROGRAM)
                require(keys[5] == Constants.TOKEN_PROGRAM_ID || keys[5] == Constants.TOKEN_2022_PROGRAM_ID)
                require(keys.size == 6 || keys[6] == Constants.SYSVAR_RENT_ADDRESS)
                require(listOf(token to recipient, feeToken to sponsor).any { (asset, owner) ->
                    !asset.isNativeSolAsset() && keys[2] == owner && keys[3] == PublicKey(asset.assetKey) &&
                        keys[1] == ata(owner, asset, keys[5])
                })
            }
            Constants.COMPUTE_BUDGET_PROGRAM_ID -> require(keys.isEmpty())
            PublicKey("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"),
            PublicKey("Memo1UhkJRfHyvLMcVucJwxXeuD728EqVDDwQDxFMNo") -> require(user !in keys)
            else -> throw IllegalArgumentException()
        }
    }
    require(transferred)
    throwIfAnyMaliciousInstruction()
}
