package one.mixin.android.api.response

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
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
import one.mixin.android.web3.Web3ChainId
import one.mixin.android.web3.Web3Exception
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.js.SolanaTxSource
import one.mixin.android.web3.js.getSolanaRpc
import org.sol4k.Constants.TOKEN_2022_PROGRAM_ID
import org.sol4k.Constants.TOKEN_PROGRAM_ID
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
@Entity(
    tableName = "web3_token",
)
@Parcelize
class Web3Token(
    @PrimaryKey
    @ColumnInfo(name = "fungible_id")
    @SerializedName("fungible_id")
    val fungibleId: String,
    @ColumnInfo(name = "name")
    @SerializedName("name")
    val name: String,
    @ColumnInfo(name = "symbol")
    @SerializedName("symbol")
    val symbol: String,
    @ColumnInfo(name = "icon_url")
    @SerializedName("icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "chain_id")
    @SerializedName("chain_id")
    val chainId: String,
    @ColumnInfo(name = "chain_name")
    @SerializedName("chain_name")
    val chainName: String,
    @ColumnInfo(name = "chain_icon_url")
    @SerializedName("chain_icon_url")
    val chainIconUrl: String,
    @ColumnInfo(name = "balance")
    @SerializedName("balance")
    val balance: String,
    @ColumnInfo(name = "price")
    @SerializedName("price")
    val price: String,
    @ColumnInfo(name = "change_absolute")
    @SerializedName("change_absolute")
    val changeAbsolute: String,
    @ColumnInfo(name = "change_percent")
    @SerializedName("change_percent")
    val changePercent: String,
    @ColumnInfo(name = "asset_key")
    @SerializedName("asset_key")
    val assetKey: String,
    @ColumnInfo(name = "decimals")
    @SerializedName("decimals")
    val decimals: Int,
) : Parcelable, Swappable {
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

    val tokenId: String
        get() {
            return chainId + assetKey
        }

    override fun toSwapToken(): SwapToken {
        return SwapToken(
            address = if (assetKey == solanaNativeTokenAssetKey) wrappedSolTokenAssetKey else assetKey,
            assetId = "",
            decimals = decimals,
            name = name,
            symbol = symbol,
            icon = iconUrl,
            chain =
            SwapChain(
                chainId = "",
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

    override fun getUnique(): String {
        return if (assetKey == solanaNativeTokenAssetKey) wrappedSolTokenAssetKey else assetKey
    }
}

const val solanaNativeTokenAssetKey = "11111111111111111111111111111111"
const val wrappedSolTokenAssetKey = "So11111111111111111111111111111111111111112"

fun Web3Token.getChainFromName(): Chain {
    return when {
        chainId.equals("ethereum", true) -> Chain.Ethereum
        chainId.equals("base", true) -> Chain.Base
        chainId.equals("blast", true) -> Chain.Blast
        chainId.equals("arbitrum", true) -> Chain.Arbitrum
        chainId.equals("optimism", true) -> Chain.Optimism
        chainId.equals("polygon", true) -> Chain.Polygon
        chainId.equals("binance-smart-chain", true) -> Chain.BinanceSmartChain
        chainId.equals("avalanche", true) -> Chain.Avalanche
        chainId.equals("solana", true) -> Chain.Solana
        else -> throw IllegalArgumentException("Not support: $chainId")
    }
}

fun Web3Token.getChainIdFromName(): String {
    return when {
        chainId.equals("ethereum", true) -> Constants.ChainId.ETHEREUM_CHAIN_ID
        chainId.equals("base", true) -> Constants.ChainId.ETHEREUM_CHAIN_ID
        chainId.equals("blast", true) -> Constants.ChainId.ETHEREUM_CHAIN_ID
        chainId.equals("arbitrum", true) -> Constants.ChainId.Arbitrum
        chainId.equals("optimism", true) -> Constants.ChainId.Optimism
        chainId.equals("polygon", true) -> Constants.ChainId.Polygon
        chainId.equals("binance-smart-chain", true) -> Constants.ChainId.BinanceSmartChain
        chainId.equals("avalanche", true) -> Constants.ChainId.Avalanche
        chainId.equals("solana", true) -> Constants.ChainId.SOLANA_CHAIN_ID
        else -> ""
    }
}

fun Web3Token.isSolana(): Boolean {
    return chainId.equals("solana", true)
}

fun Web3Token.getWeb3ChainId(): Int {
    return when {
        chainId.equals("ethereum", true) -> Web3ChainId.EthChainId
        chainId.equals("base", true) -> Web3ChainId.BaseChainId
        chainId.equals("blast", true) -> Web3ChainId.BlastChainId
        chainId.equals("arbitrum", true) -> Web3ChainId.ArbitrumChainId
        chainId.equals("optimism", true) -> Web3ChainId.OptimismChainId
        chainId.equals("polygon", true) -> Web3ChainId.PolygonChainId
        chainId.equals("binance-smart-chain", true) -> Web3ChainId.BscChainId
        chainId.equals("avalanche", true) -> Web3ChainId.AvalancheChainId
        chainId.equals("solana", true) -> Web3ChainId.SolanaChainId
        else -> Web3ChainId.MixinChainId
    }
}

fun Web3Token.isSolToken(): Boolean {
    return isSolana() && (assetKey == solanaNativeTokenAssetKey || assetKey == wrappedSolTokenAssetKey)
}

private fun Web3Token.getChainAssetKey(): String {
    return if (chainId.equals("ethereum", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("base", true)) {
        "0x0000000000000000000000000000000000000000"}
    else if (chainId.equals("blast", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("arbitrum", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("optimism", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("polygon", true)) {
        "0x0000000000000000000000000000000000001010"
    } else if (chainId.equals("binance-smart-chain", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("avalanche", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("solana", true)) {
        solanaNativeTokenAssetKey
    } else {
        ""
    }
}

fun Web3Token.supportDepositFromMixin(): Boolean {
    return when (chainId.lowercase(Locale.US)) {
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
            if (tokenAmount.decimals != decimals) {
                throw Web3Exception(Web3Exception.ErrorCode.InvalidWeb3Token, "Web3Token decimals $decimals not equal rpc decimals ${tokenAmount.decimals}")
            }
            val (sendAssociatedAccount) = PublicKey.findProgramDerivedAddress(sender, tokenMintAddress, tokenProgramId)
            instructions.add(
                SplTransferInstruction(
                    from = sendAssociatedAccount,
                    to = receiveAssociatedAccount,
                    mint = tokenMintAddress,
                    owner = sender,
                    signers = emptyList(),
                    amount = BigDecimal(v).multiply(BigDecimal.TEN.pow(decimals)).toLong(),
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
        val transaction =
            if ((chainId.equals("ethereum", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId.equals("base", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId.equals("blast", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId.equals("arbitrum", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId.equals("optimism", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId.equals("polygon", true) && assetKey == "0x0000000000000000000000000000000000001010") ||
                (chainId.equals("binance-smart-chain", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId.equals("avalanche", true) && assetKey == "0x0000000000000000000000000000000000000000")
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

fun Web3Token.copy(
    fungibleId: String = this.fungibleId,
    name: String = this.name,
    symbol: String = this.symbol,
    iconUrl: String = this.iconUrl,
    chainId: String = this.chainId,
    chainName: String = this.chainName,
    chainIconUrl: String = this.chainIconUrl,
    balance: String = this.balance,
    price: String = this.price,
    changeAbsolute: String = this.changeAbsolute,
    changePercent: String = this.changePercent,
    assetKey: String = this.assetKey,
    decimals: Int = this.decimals
): Web3Token {
    return Web3Token(
        fungibleId = fungibleId,
        name = name,
        symbol = symbol,
        iconUrl = iconUrl,
        chainId = chainId,
        chainName = chainName,
        chainIconUrl = chainIconUrl,
        balance = balance,
        price = price,
        changeAbsolute = changeAbsolute,
        changePercent = changePercent,
        assetKey = assetKey,
        decimals = decimals
    )
}

fun Long.solLamportToAmount(scale: Int = 9): BigDecimal {
    return BigDecimal(this).divide(BigDecimal.TEN.pow(9)).setScale(scale, RoundingMode.CEILING)
}
