package one.mixin.android.tip.wc.internal

import com.github.salomonbrys.kotson.jsonDeserializer

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
