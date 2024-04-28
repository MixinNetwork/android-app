package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.web3.js.JsSignMessage
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Uint
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
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
    val decimals:Int,
) : Parcelable

fun Web3Token.getChainFromName(): Chain {
    return when {
        chainId.equals("ethereum", true) -> Chain.Ethereum
        chainId.equals("base", true) -> Chain.Base
        chainId.equals("arbitrum", true) -> Chain.Arbitrum
        chainId.equals("optimism", true) -> Chain.Optimism
        chainId.equals("polygon", true) -> Chain.Polygon
        chainId.equals("binance-smart-chain", true) -> Chain.BinanceSmartChain
        chainId.equals("avalanche", true) -> Chain.Avalanche
        else -> throw IllegalArgumentException("Not support: $chainId")
    }
}

fun Web3Token.getChainIdFromName(): String {
    return when {
        chainId.equals("ethereum", true) -> Constants.ChainId.ETHEREUM_CHAIN_ID
        chainId.equals("base", true) -> Constants.ChainId.ETHEREUM_CHAIN_ID
        chainId.equals("arbitrum", true) -> Constants.ChainId.ETHEREUM_CHAIN_ID
        chainId.equals("optimism", true) -> Constants.ChainId.ETHEREUM_CHAIN_ID
        chainId.equals("polygon", true) -> Constants.ChainId.Polygon
        chainId.equals("binance-smart-chain", true) -> Constants.ChainId.BinanceSmartChain
        chainId.equals("avalanche", true) -> Constants.ChainId.Avalanche
        else -> throw IllegalArgumentException("Not support: $chainId")
    }
}

fun Web3Token.getChainAssetKey(): String {
    return if (chainId.equals("ethereum", true)) "0x0000000000000000000000000000000000000000"
    else if (chainId.equals("base", true)) "0x0000000000000000000000000000000000000000"
    else if (chainId.equals("arbitrum", true)) "0x0000000000000000000000000000000000000000"
    else if (chainId.equals("optimism", true)) "0x0000000000000000000000000000000000000000"
    else if (chainId.equals("polygon", true)) "0x0000000000000000000000000000000000001010"
    else if (chainId.equals("binance-smart-chain", true)) "0x0000000000000000000000000000000000000000"
    else if (chainId.equals("avalanche", true)) "0x0000000000000000000000000000000000000000"
    else throw IllegalArgumentException("Not support: $chainId")
}

fun Web3Token.supportDeposit(): Boolean {
    return when (chainId.lowercase(Locale.US)) {
        "ethereum", "base", "arbitrum", "optimism", "polygon", "binance-smart-chain", "avalanche" -> true
        else -> false
    }
}

fun Web3Token.findChainToken(tokens: List<Web3Token>): Web3Token? {
    val chainAssetKey = getChainAssetKey()
    return tokens.firstOrNull { token ->
        token.chainId == chainId && token.assetKey == chainAssetKey
    }
}

fun Web3Token.buildTransaction(fromAddress: String, toAddress: String, v: String): JsSignMessage {
    val transaction =
        if ((chainId.equals("ethereum", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
            (chainId.equals("base", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
            (chainId.equals("arbitrum", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
            (chainId.equals("optimism", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
            (chainId.equals("polygon", true) && assetKey == "0x0000000000000000000000000000000000001010") ||
            (chainId.equals("binance-smart-chain", true) && assetKey == "0x0000000000000000000000000000000000000000")||
            (chainId.equals("avalanche", true) && assetKey == "0x0000000000000000000000000000000000000000")
        ) {
        val value = Numeric.toHexStringWithPrefix(Convert.toWei(v, Convert.Unit.ETHER).toBigInteger())
        WCEthereumTransaction(fromAddress, toAddress, null, null, null, null, null, null, value, null)
    } else {
        val function = Function(
            "transfer",
            listOf(
                Address(toAddress), Uint(
                    BigDecimal(v).multiply(BigDecimal.TEN.pow(decimals)).toBigInteger()
                )
            ),
            emptyList()
        )
        val data = FunctionEncoder.encode(function)
        WCEthereumTransaction(fromAddress, assetKey, null, null, null, null, null, null, "0x0", data)
    }
    return JsSignMessage(0, JsSignMessage.TYPE_TRANSACTION, transaction)
}