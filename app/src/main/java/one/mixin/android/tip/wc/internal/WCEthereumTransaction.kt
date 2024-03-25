package one.mixin.android.tip.wc.internal

import com.github.salomonbrys.kotson.jsonDeserializer
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.utils.Numeric
import java.math.BigInteger

data class WCEthereumTransaction(
    val from: String,
    val to: String?,
    val nonce: String?,
    val gasPrice: String?,
    val maxFeePerGas: String?,
    val maxPriorityFeePerGas: String?,
    val gas: String?,
    val gasLimit: String?,
    val value: String?,
    val data: String?,
)

fun WCEthereumTransaction.isLegacy(): Boolean {
    return maxFeePerGas == null && maxPriorityFeePerGas == null
}

val ethTransactionSerializer =
    jsonDeserializer<List<WCEthereumTransaction>> {
        val array = mutableListOf<WCEthereumTransaction>()
        it.json.asJsonArray.forEach { tx ->
            if (tx.isJsonObject) {
                array.add(it.context.deserialize(tx))
            }
        }
        array
    }
fun WCEthereumTransaction.toTransaction(): Transaction {
    return Transaction(from,
        Numeric.toBigInt(nonce ?: "0x00"),
        Numeric.toBigInt(gasPrice ?: "0x00"),
        Numeric.toBigInt(gasLimit ?: "0x00"),
        to,
        Numeric.toBigInt(value ?: "0x00"),
        data)
}