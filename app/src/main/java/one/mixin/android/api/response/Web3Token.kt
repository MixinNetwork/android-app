package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.web3.JsSignMessage
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Uint
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal

@Parcelize
class Web3Token(
    @SerializedName("id")
    val id: String,
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
        chainName.contains("Ethereum", true) -> Chain.Ethereum
        chainName.contains("Polygon", true) -> Chain.Polygon
        chainName.contains("BNB Chain", true) -> Chain.BinanceSmartChain
        else -> throw IllegalArgumentException("Not support: $chainName")
    }
}

fun Web3Token.buildTransaction(fromAddress: String, toAddress: String, v: String): JsSignMessage {
    val transaction = if (symbol == "ETH" || symbol == "MATIC" || symbol == "BNB") {
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