package one.mixin.android.web3

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.db.web3.vo.Web3TokenItem
import org.sol4k.Constants.TOKEN_2022_PROGRAM_ID
import org.sol4k.Constants.TOKEN_PROGRAM_ID
import org.sol4k.PublicKey
import java.math.BigDecimal

val SOLANA_RENT_EXEMPTION: BigDecimal = BigDecimal("0.00089088")
val SOLANA_TOKEN_ACCOUNT_RENT_EXEMPTION: BigDecimal = BigDecimal("0.00203928")

enum class SolanaRecipientAccountState {
    EXISTS,
    NEEDS_SYSTEM_ACCOUNT,
    NEEDS_TOKEN_ACCOUNT,
}

data class SolanaTransferAmountRange(
    val minAmount: BigDecimal,
    val maxAmount: BigDecimal,
) {
    fun canTransfer(amount: BigDecimal): Boolean {
        return maxAmount >= minAmount && amount >= minAmount && amount <= maxAmount
    }
}

fun isNativeSolAsset(chainId: String?, assetId: String?): Boolean {
    return chainId == Constants.ChainId.SOLANA_CHAIN_ID && assetId == Constants.ChainId.SOLANA_CHAIN_ID
}

fun SwapToken.isNativeSolAsset(): Boolean = isNativeSolAsset(chain.chainId, assetId)

fun Web3TokenItem.isNativeSolAsset(): Boolean = isNativeSolAsset(chainId, assetId)

fun nativeSolSpendableBalance(
    balance: BigDecimal,
    solFee: BigDecimal = BigDecimal.ZERO,
    extraReserved: BigDecimal = BigDecimal.ZERO,
    allowZeroBalance: Boolean = false,
): BigDecimal {
    val remaining = balance
        .subtract(solFee)
        .subtract(extraReserved)

    return if (allowZeroBalance) {
        remaining.max(BigDecimal.ZERO)
    } else {
        remaining.subtract(SOLANA_RENT_EXEMPTION).max(BigDecimal.ZERO)
    }
}

fun hasSolBalanceAfterFeeAndRent(
    balance: BigDecimal,
    solFee: BigDecimal,
    extraReserved: BigDecimal = BigDecimal.ZERO,
    allowZeroBalance: Boolean = false,
): Boolean {
    val remaining = balance.subtract(solFee).subtract(extraReserved)
    return remaining >= SOLANA_RENT_EXEMPTION || (allowZeroBalance && remaining.compareTo(BigDecimal.ZERO) == 0)
}

fun requiredSolBalance(
    transferAmount: BigDecimal,
    solFee: BigDecimal,
    sendingNativeSol: Boolean,
): BigDecimal {
    return if (sendingNativeSol) {
        transferAmount.add(solFee).add(SOLANA_RENT_EXEMPTION)
    } else {
        solFee.add(SOLANA_RENT_EXEMPTION)
    }
}

fun solanaTransferAmountRange(
    token: Web3TokenItem,
    feeToken: Web3TokenItem?,
    feeAmount: BigDecimal,
    recipientAccountState: SolanaRecipientAccountState,
    allowZeroBalance: Boolean = false,
    includeAtaCreationReserve: Boolean = false,
): SolanaTransferAmountRange {
    val tokenBalance = token.balance.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val feeTokenBalance = feeToken?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val minAmount = if (token.isNativeSolAsset() && recipientAccountState == SolanaRecipientAccountState.NEEDS_SYSTEM_ACCOUNT) {
        SOLANA_RENT_EXEMPTION
    } else {
        BigDecimal.ZERO
    }
    // Native flow always uses SOL as fee, so feeToken.isNativeSolAsset() is always true there.
    // Gasless flow passes includeAtaCreationReserve = false, so ataReserve is always zero.
    val ataReserve = if (
        includeAtaCreationReserve &&
        recipientAccountState == SolanaRecipientAccountState.NEEDS_TOKEN_ACCOUNT &&
        feeToken?.isNativeSolAsset() == true
    ) {
        SOLANA_TOKEN_ACCOUNT_RENT_EXEMPTION
    } else {
        BigDecimal.ZERO
    }
    val maxAmount = when {
        token.isNativeSolAsset() -> {
            val feeTokenEnough = when {
                feeToken == null -> false
                feeToken.assetId == token.assetId -> true
                feeToken.isNativeSolAsset() -> {
                    hasSolBalanceAfterFeeAndRent(
                        balance = feeTokenBalance,
                        solFee = feeAmount,
                        extraReserved = ataReserve,
                        allowZeroBalance = allowZeroBalance,
                    )
                }
                else -> {
                    feeTokenBalance >= feeAmount
                }
            }
            if (!feeTokenEnough) {
                BigDecimal.ZERO
            } else {
                val solFee = if (feeToken?.assetId == token.assetId) feeAmount else BigDecimal.ZERO
                nativeSolSpendableBalance(
                    balance = tokenBalance,
                    solFee = solFee,
                    allowZeroBalance = allowZeroBalance,
                )
            }
        }
        feeToken == null -> {
            BigDecimal.ZERO
        }
        feeToken.assetId == token.assetId -> {
            tokenBalance.subtract(feeAmount).max(BigDecimal.ZERO)
        }
        feeToken.isNativeSolAsset() -> {
            if (hasSolBalanceAfterFeeAndRent(feeTokenBalance, feeAmount, extraReserved = ataReserve, allowZeroBalance = allowZeroBalance)) {
                tokenBalance
            } else {
                BigDecimal.ZERO
            }
        }
        feeTokenBalance >= feeAmount -> {
            tokenBalance
        }
        else -> {
            BigDecimal.ZERO
        }
    }

    return SolanaTransferAmountRange(
        minAmount = minAmount,
        maxAmount = maxAmount.max(BigDecimal.ZERO),
    )
}

suspend fun Web3TokenItem.solanaRecipientAccountState(
    rpc: Rpc,
    toAddress: String,
): SolanaRecipientAccountState {
    if (chainId != Constants.ChainId.SOLANA_CHAIN_ID) {
        return SolanaRecipientAccountState.EXISTS
    }

    val receiver = PublicKey(toAddress)
    if (isNativeSolAsset()) {
        return if (rpc.getAccountInfo(receiver) != null) {
            SolanaRecipientAccountState.EXISTS
        } else {
            SolanaRecipientAccountState.NEEDS_SYSTEM_ACCOUNT
        }
    }

    val tokenMintAddress = PublicKey(assetKey)
    val tokenMintAccount = rpc.getAccountInfo(tokenMintAddress)
        ?: throw IllegalStateException("rpc getAccountInfo $assetKey is null")
    val tokenProgramId = when (tokenMintAccount.owner) {
        TOKEN_PROGRAM_ID -> TOKEN_PROGRAM_ID
        TOKEN_2022_PROGRAM_ID -> TOKEN_2022_PROGRAM_ID
        else -> throw IllegalStateException("invalid account owner ${tokenMintAccount.owner}")
    }
    val (receiveAssociatedAccount) = PublicKey.findProgramDerivedAddress(receiver, tokenMintAddress, tokenProgramId)

    return if (rpc.getAccountInfo(receiveAssociatedAccount) != null) {
        SolanaRecipientAccountState.EXISTS
    } else {
        SolanaRecipientAccountState.NEEDS_TOKEN_ACCOUNT
    }
}
