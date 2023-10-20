package one.mixin.android.vo.utxo

import one.mixin.android.vo.Output
import kernel.Utxo
import java.util.UUID

class Change(
    val hash: String,
    val index: Int,
    val amount: String,
)

fun changeToOutput(change: Utxo, asset: String, createdAt: String): Output {
    val outputId = UUID.nameUUIDFromBytes("${change.hash}:${change.index}".toByteArray()).toString()
    return Output(outputId, change.hash, change.index.toInt(), asset, change.amount, "", emptyList(), "", 1, emptyList(), "", "unspent", createdAt, "", "", "", "")
}