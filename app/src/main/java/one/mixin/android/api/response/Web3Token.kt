package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants
import one.mixin.android.api.request.web3.PriorityLevel
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.extension.base64Encode
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.web3.Web3Exception
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.js.getSolanaRpc
import org.sol4k.PublicKey
import org.sol4k.Transaction
import org.sol4k.VersionedTransaction
import org.sol4k.instruction.CreateAssociatedTokenAccountInstruction
import org.sol4k.instruction.Instruction
import org.sol4k.instruction.SplTransferInstruction
import org.sol4k.instruction.TransferInstruction
import org.sol4k.lamportToSol
import org.sol4k.solToLamport
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Uint
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Parcelize
class Web3Token(
    @SerializedName("fungible_id")
    val fungibleId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("chain_name")
    val chainName: String,
    @SerializedName("chain_icon_url")
    val chainIconUrl: String,
    @SerializedName("balance")
    val balance: String,
    @SerializedName("price")
    val price: String,
    @SerializedName("change_absolute")
    val changeAbsolute: String,
    @SerializedName("change_percent")
    val changePercent: String,
    @SerializedName("asset_key")
    val assetKey: String,
    @SerializedName("decimals")
    val decimals: Int,
) : Parcelable {
    fun toLongAmount(amount: String): Long {
        val a =
            try {
                BigDecimal(amount)
            } catch (e: Exception) {
                return 0
            }
        return a.multiply(BigDecimal.TEN.pow(decimals)).toLong()
    }

    fun toStringAmount(amount: Long): String {
        return realAmount(amount).stripTrailingZeros().toPlainString()
    }

    fun realAmount(amount: Long): BigDecimal {
        return BigDecimal(amount).divide(BigDecimal.TEN.pow(decimals)).setScale(9, RoundingMode.CEILING)
    }
}

const val solanaNativeTokenAssetKey = "11111111111111111111111111111111"
const val wrappedSolTokenAssetKey = "So11111111111111111111111111111111111111112"

fun Web3Token.toSwapToken(): SwapToken {
    return SwapToken(
        address = if (assetKey == solanaNativeTokenAssetKey) wrappedSolTokenAssetKey else assetKey,
        decimals = decimals,
        name = name,
        symbol = symbol,
        icon = iconUrl,
        chain =
            SwapChain(
                decimals = 0,
                name = chainName,
                symbol = symbol,
                icon = chainIconUrl,
                price = null,
            ),
        balance = balance,
        price = price,
    )
}

fun Web3Token.getChainFromName(): Chain {
    return when {
        chainName.equals("ethereum", true) -> Chain.Ethereum
        chainName.equals("base", true) -> Chain.Base
        chainName.equals("arbitrum", true) -> Chain.Arbitrum
        chainName.equals("optimism", true) -> Chain.Optimism
        chainName.equals("polygon", true) -> Chain.Polygon
        chainName.equals("binance-smart-chain", true) -> Chain.BinanceSmartChain
        chainName.equals("avalanche", true) -> Chain.Avalanche
        chainName.equals("solana", true) -> Chain.Solana
        else -> throw IllegalArgumentException("Not support: $chainName")
    }
}

fun Web3Token.getChainIdFromName(): String {
    return when {
        chainName.equals("ethereum", true) -> Constants.ChainId.ETHEREUM_CHAIN_ID
        chainName.equals("base", true) -> Constants.ChainId.ETHEREUM_CHAIN_ID
        chainName.equals("arbitrum", true) -> Constants.ChainId.Arbitrum
        chainName.equals("optimism", true) -> Constants.ChainId.Optimism
        chainName.equals("polygon", true) -> Constants.ChainId.Polygon
        chainName.equals("binance-smart-chain", true) -> Constants.ChainId.BinanceSmartChain
        chainName.equals("avalanche", true) -> Constants.ChainId.Avalanche
        chainName.equals("solana", true) -> Constants.ChainId.SOLANA_CHAIN_ID
        else -> ""
    }
}

fun Web3Token.isSolana(): Boolean {
    return chainName.equals("solana", true)
}

fun Web3Token.isSolToken(): Boolean {
    return isSolana() && (assetKey == solanaNativeTokenAssetKey || assetKey == wrappedSolTokenAssetKey)
}

private fun Web3Token.getChainAssetKey(): String {
    return if (chainName.equals("ethereum", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainName.equals("base", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainName.equals("arbitrum", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainName.equals("optimism", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainName.equals("polygon", true)) {
        "0x0000000000000000000000000000000000001010"
    } else if (chainName.equals("binance-smart-chain", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainName.equals("avalanche", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainName.equals("solana", true)) {
        solanaNativeTokenAssetKey
    } else {
        ""
    }
}

fun Web3Token.supportDepositFromMixin(): Boolean {
    return when (chainName.lowercase(Locale.US)) {
        "ethereum", "polygon", "binance-smart-chain", "solana" -> true
        else -> false
    }
}

fun Web3Token.findChainToken(tokens: List<Web3Token>): Web3Token? {
    val chainAssetKey = getChainAssetKey()
    return tokens.firstOrNull { token ->
        token.chainId == chainId && token.assetKey == chainAssetKey
    }
}

fun Web3Token.calcSolBalanceChange(balanceChange: VersionedTransaction.TokenBalanceChange): String {
    return if (isSolToken()) {
        lamportToSol(BigDecimal(balanceChange.change))
    } else {
        BigDecimal(balanceChange.change).divide(BigDecimal.TEN.pow(decimals)).setScale(decimals, RoundingMode.CEILING)
    }.stripTrailingZeros().toPlainString()
}

suspend fun Web3Token.buildTransaction(
    fromAddress: String,
    toAddress: String,
    v: String,
): JsSignMessage {
    if (chainName.equals("solana", true)) {
        JsSigner.useSolana()
        val sender = PublicKey(fromAddress)
        val receiver = PublicKey(toAddress)
        val instructions = mutableListOf<Instruction>()
        val conn = getSolanaRpc()
        if (isSolToken()) {
            val amount = solToLamport(v).toLong()
            instructions.add(TransferInstruction(sender, receiver, amount))
        } else {
            val tokenMintAddress = PublicKey(assetKey)
            val (receiveAssociatedAccount) = PublicKey.findProgramDerivedAddress(receiver, tokenMintAddress)
            val receiveAssociatedAccountInfo =
                withContext(Dispatchers.IO) {
                    conn.getAccountInfo(receiveAssociatedAccount)
                }
            if (receiveAssociatedAccountInfo == null) {
                instructions.add(
                    CreateAssociatedTokenAccountInstruction(
                        payer = sender,
                        associatedToken = receiveAssociatedAccount,
                        owner = receiver,
                        mint = tokenMintAddress,
                    ),
                )
            }
            val tokenAmount =
                withContext(Dispatchers.IO) {
                    conn.getTokenSupply(assetKey)
                }
            if (tokenAmount == null) {
                throw Web3Exception(Web3Exception.ErrorCode.InvalidWeb3Token, "rpc getTokenSupply Web3Token $assetKey is null")
            }
            if (tokenAmount.decimals != decimals) {
                throw Web3Exception(Web3Exception.ErrorCode.InvalidWeb3Token, "Web3Token decimals $decimals not equal rpc decimals ${tokenAmount.decimals}")
            }
            val (sendAssociatedAccount) = PublicKey.findProgramDerivedAddress(sender, tokenMintAddress)
            instructions.add(
                SplTransferInstruction(
                    from = sendAssociatedAccount,
                    to = receiveAssociatedAccount,
                    mint = tokenMintAddress,
                    owner = sender,
                    signers = emptyList(),
                    amount = BigDecimal(v).multiply(BigDecimal.TEN.pow(decimals)).toLong(),
                    decimals = tokenAmount.decimals,
                ),
            )
        }
        val transaction =
            Transaction(
                toAddress, // use address as temp placeholder, will replace when signing
                instructions,
                sender,
            )
        transaction.addPlaceholderSignature()
        val tx = transaction.serialize().base64Encode()
        return JsSignMessage(0, JsSignMessage.TYPE_RAW_TRANSACTION, data = tx, priorityLevel = PriorityLevel.Medium)
    } else {
        JsSigner.useEvm()
        val transaction =
            if ((chainName.equals("ethereum", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainName.equals("base", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainName.equals("arbitrum", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainName.equals("optimism", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainName.equals("polygon", true) && assetKey == "0x0000000000000000000000000000000000001010") ||
                (chainName.equals("binance-smart-chain", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainName.equals("avalanche", true) && assetKey == "0x0000000000000000000000000000000000000000")
            ) {
                val value = Numeric.toHexStringWithPrefix(Convert.toWei(v, Convert.Unit.ETHER).toBigInteger())
                WCEthereumTransaction(fromAddress, toAddress, null, null, null, null, null, null, value, null)
            } else {
                val function =
                    Function(
                        "transfer",
                        listOf(
                            Address(toAddress),
                            Uint(
                                BigDecimal(v).multiply(BigDecimal.TEN.pow(decimals)).toBigInteger(),
                            ),
                        ),
                        emptyList(),
                    )
                val data = FunctionEncoder.encode(function)
                WCEthereumTransaction(fromAddress, assetKey, null, null, null, null, null, null, "0x0", data)
            }
        return JsSignMessage(0, JsSignMessage.TYPE_TRANSACTION, transaction)
    }
}
