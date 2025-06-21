package one.mixin.android.tip.wc.internal

import android.os.Parcelable
import com.github.salomonbrys.kotson.jsonDeserializer
import kotlinx.parcelize.Parcelize
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import org.web3j.utils.Convert

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
) : Parcelable {
    fun getMainTokenAmount(): BigDecimal {
        return value?.let {
            try {
                val wei = BigInteger(it.removePrefix("0x"), 16)
                Convert.fromWei(wei.toString(), Convert.Unit.ETHER)
            } catch (e: Exception) {
                BigDecimal.ZERO
            }
        } ?: BigDecimal.ZERO
    }
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
    return Transaction(
        from,
        nonce?.let { Numeric.decodeQuantity(it) },
        gasPrice?.let { Numeric.decodeQuantity(it) },
        gasLimit?.let { Numeric.decodeQuantity(it) },
        to,
        value?.let { Numeric.decodeQuantity(it) },
        data,
    )
}
