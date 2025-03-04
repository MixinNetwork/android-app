package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Swappable
import one.mixin.android.extension.base64Encode
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.vo.Fiats
import one.mixin.android.web3.Web3Exception
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.js.SolanaTxSource
import one.mixin.android.web3.js.getSolanaRpc
import org.sol4k.Constants.TOKEN_2022_PROGRAM_ID
import org.sol4k.Constants.TOKEN_PROGRAM_ID
import org.sol4k.PublicKey
import org.sol4k.Transaction
import org.sol4k.instruction.CreateAssociatedTokenAccountInstruction
import org.sol4k.instruction.Instruction
import org.sol4k.instruction.SplTransferInstruction
import org.sol4k.instruction.TransferInstruction
import org.sol4k.solToLamport
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Uint
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.RoundingMode

@Parcelize
data class Web3TokenItem(
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "asset_key")
    val assetKey: String,
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "precision")
    val precision: Int,
    @ColumnInfo(name = "kernel_asset_id")
    val kernelAssetId: String = "",
    @ColumnInfo(name = "amount")
    val balance: String,
    @ColumnInfo(name = "price_usd")
    val priceUsd: String,
    @ColumnInfo(name = "change_usd")
    val changeUsd: String,
    @ColumnInfo(name = "chain_icon_url")
    val chainIcon: String?,
    @ColumnInfo(name = "chain_name")
    val chainName: String?,
    @ColumnInfo(name = "chain_symbol")
    val chainSymbol: String?,
) : Parcelable, Swappable {
    
    fun getChainDisplayName(): String {
        return chainName ?: when {
            chainId == Constants.ChainId.ETHEREUM_CHAIN_ID -> "Ethereum"
            chainId == Constants.ChainId.Base -> "ETH"
            // chainId.equals("blast", true) -> "Blast"
            // chainId.equals("arbitrum", true) -> "Arbitrum"
            // chainId.equals("optimism", true) -> "Optimism"
            chainId == Constants.ChainId.BinanceSmartChain -> "Polygon"
            chainId == Constants.ChainId.Polygon -> "BNB Chain"
            chainId == Constants.ChainId.Avalanche -> "Avalanche"
            chainId == Constants.ChainId.SOLANA_CHAIN_ID -> "Solana"
            else -> chainId
        }
    }
    
    fun isSolana(): Boolean {
        return chainId.equals(Constants.ChainId.SOLANA_CHAIN_ID, true)
    }
    
    override fun toSwapToken(): SwapToken {
        return SwapToken(
            address = if (assetKey == solanaNativeTokenAssetKey) wrappedSolTokenAssetKey else assetKey,
            assetId = assetId,
            decimals = precision,
            name = name,
            symbol = symbol,
            icon = iconUrl,
            chain = SwapChain(
                chainId = chainId,
                decimals = precision,
                name = getChainDisplayName(),
                symbol = chainSymbol ?: "",
                icon = chainIcon ?: "",
                price = null,
            ),
            balance = balance,
            price = priceUsd,
        )
    }
    
    override fun getUnique(): String {
        return if (assetKey == solanaNativeTokenAssetKey) wrappedSolTokenAssetKey else assetKey
    }

    fun toStringAmount(amount: Long): String {
        return realAmount(amount).stripTrailingZeros().toPlainString()
    }

    fun realAmount(amount: Long): BigDecimal {
        return BigDecimal(amount).divide(BigDecimal.TEN.pow(precision)).setScale(9, RoundingMode.CEILING)
    }

    fun btcValue(btcPrice: BigDecimal): BigDecimal = try {
        BigDecimal(balance).multiply(btcPrice)
    } catch (e: NumberFormatException) {
        BigDecimal.ZERO
    }
    
    fun fiat(): BigDecimal = try {
        if (balance.isBlank()) BigDecimal.ZERO
        else BigDecimal(balance).multiply(priceFiat())
    } catch (e: NumberFormatException) {
        BigDecimal.ZERO
    }

    fun priceFiat(): BigDecimal = when (priceUsd) {
        "0" -> BigDecimal.ZERO
        "" -> BigDecimal.ZERO
        else -> BigDecimal(priceUsd).multiply(Fiats.getRate().toBigDecimal())
    }

    fun findChainToken(tokens: List<Web3TokenItem>): Web3TokenItem? {
        val chainAssetKey = when {
            // Todo
            chainId.equals("solana", true) && assetKey == solanaNativeTokenAssetKey -> wrappedSolTokenAssetKey
            else -> assetKey
        }
        return tokens.firstOrNull { token ->
            token.chainId == chainId && token.assetKey == chainAssetKey
        }
    }

    fun Long.solLamportToAmount(scale: Int = 9): BigDecimal {
        return BigDecimal(this).divide(BigDecimal.TEN.pow(9)).setScale(scale, RoundingMode.CEILING)
    }
}

fun Web3TokenItem.getChainFromName(): Chain {
    return when {
        chainId == Constants.ChainId.ETHEREUM_CHAIN_ID -> Chain.Ethereum
        chainId == Constants.ChainId.Base -> Chain.Base
        // chainId.equals("blast", true) -> Chain.Blast
        // chainId.equals("arbitrum", true) -> Chain.Arbitrum
        // chainId.equals("optimism", true) -> Chain.Optimism
        chainId == Constants.ChainId.Polygon-> Chain.Polygon
        chainId == Constants.ChainId.BinanceSmartChain-> Chain.BinanceSmartChain
        chainId == Constants.ChainId.Avalanche -> Chain.Avalanche
        chainId == Constants.ChainId.SOLANA_CHAIN_ID -> Chain.Solana
        else -> throw IllegalArgumentException("Not support: $chainId")
    }
}

fun Web3TokenItem.getChainSymbolFromName(): String {
    return when {
        chainId == Constants.ChainId.ETHEREUM_CHAIN_ID -> "ETH"
        chainId == Constants.ChainId.Base -> "ETH"
        // chainId.equals("blast", true) -> "ETH"
        // chainId.equals("arbitrum", true) -> "ETH"
        // chainId.equals("optimism", true) -> "ETH"
        chainId == Constants.ChainId.BinanceSmartChain -> "POL"
        chainId == Constants.ChainId.Polygon -> "BNB"
        chainId == Constants.ChainId.Avalanche -> "AVAX"
        chainId == Constants.ChainId.SOLANA_CHAIN_ID -> "SOL"
        else -> throw IllegalArgumentException("Not support: $chainId")
    }
}


suspend fun Web3TokenItem.buildTransaction(
    fromAddress: String,
    toAddress: String,
    v: String,
): JsSignMessage {
    if (chainId.equals("solana", true)) {
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
            val tokenMintAccount = withContext(Dispatchers.IO) {
                conn.getAccountInfo(tokenMintAddress)
            }
            if (tokenMintAccount == null) {
                throw Web3Exception(Web3Exception.ErrorCode.InvalidWeb3Token, "rpc getAccountInfo $assetKey is null")
            }
            val tokenProgramId = if (tokenMintAccount.owner == TOKEN_PROGRAM_ID) {
                TOKEN_PROGRAM_ID
            } else if (tokenMintAccount.owner == TOKEN_2022_PROGRAM_ID) {
                TOKEN_2022_PROGRAM_ID
            } else {
                throw Web3Exception(Web3Exception.ErrorCode.InvalidWeb3Token, "invalid account owner ${tokenMintAccount.owner}")
            }

            val (receiveAssociatedAccount) = PublicKey.findProgramDerivedAddress(receiver, tokenMintAddress, tokenProgramId)
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
                        tokenProgramId,
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
            if (tokenAmount.decimals != precision) {
                throw Web3Exception(Web3Exception.ErrorCode.InvalidWeb3Token, "Web3Token decimals $precision not equal rpc decimals ${tokenAmount.decimals}")
            }
            val (sendAssociatedAccount) = PublicKey.findProgramDerivedAddress(sender, tokenMintAddress, tokenProgramId)
            instructions.add(
                SplTransferInstruction(
                    from = sendAssociatedAccount,
                    to = receiveAssociatedAccount,
                    mint = tokenMintAddress,
                    owner = sender,
                    signers = emptyList(),
                    amount = BigDecimal(v).multiply(BigDecimal.TEN.pow(precision)).toLong(),
                    decimals = tokenAmount.decimals,
                    tokenProgramId = tokenProgramId,
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
        return JsSignMessage(0, JsSignMessage.TYPE_RAW_TRANSACTION, data = tx, solanaTxSource = SolanaTxSource.InnerTransfer)
    } else {
        JsSigner.useEvm()
        // (chainId.equals("base", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
        // (chainId.equals("blast", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
        // (chainId.equals("arbitrum", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
        // (chainId.equals("optimism", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
        val transaction =
            if ((chainId == Constants.ChainId.ETHEREUM_CHAIN_ID && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId == Constants.ChainId.Polygon && assetKey == "0x0000000000000000000000000000000000001010") ||
                (chainId == Constants.ChainId.BinanceSmartChain && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId == Constants.ChainId.Avalanche && assetKey == "0x0000000000000000000000000000000000000000")
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
                                BigDecimal(v).multiply(BigDecimal.TEN.pow(precision)).toBigInteger(),
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