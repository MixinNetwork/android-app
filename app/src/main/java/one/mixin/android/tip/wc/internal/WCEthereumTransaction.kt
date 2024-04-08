package one.mixin.android.tip.wc.internal

import android.os.Parcelable
import com.github.salomonbrys.kotson.jsonDeserializer
import kotlinx.parcelize.Parcelize
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.utils.Numeric

@Parcelize
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
) : Parcelable

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
    return Transaction(
        from,
        nonce?.let { Numeric.toBigInt(it) },
        gasPrice?.let { Numeric.toBigInt(it) },
        gasLimit?.let { Numeric.toBigInt(it) },
        to,
        value?.let { Numeric.toBigInt(it) },
        data,
    )
}
