package one.mixin.android.vo.utxo

import one.mixin.android.vo.safe.Output
import kernel.Utxo
import java.lang.Exception
import java.util.UUID

class Change(
    val hash: String,
    val index: Int,
    val amount: String,
)

fun changeToOutput(change: Utxo, asset: String, mask: String, keys: List<String>, createdAt: String): Output {
    if(mask.isEmpty() || keys.isEmpty()) {
        throw Exception("bad mask and keys")
    }
    val outputId = UUID.nameUUIDFromBytes("${change.hash}:${change.index}".toByteArray()).toString()
    return Output(
        outputId, change.hash, change.index.toInt(), asset, 0, change.amount, mask, keys,  emptyList(), "", 1, "","unspent", createdAt, "", "", "", ""
    )
}