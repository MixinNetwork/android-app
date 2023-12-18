package one.mixin.android.vo.utxo

import kernel.Utxo
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.safe.Output
import one.mixin.android.vo.safe.OutputState
import java.util.UUID

class Change(
    val hash: String,
    val index: Int,
    val amount: String,
)

fun changeToOutput(
    change: Utxo,
    asset: String,
    mask: String,
    keys: List<String>,
    lastOutput: Output,
): Output {
    if (mask.isEmpty() || keys.isEmpty()) {
        throw Exception("bad mask and keys")
    }
    val outputId = UUID.nameUUIDFromBytes("${change.hash}:${change.index}".toByteArray()).toString()
    return Output(
        outputId, change.hash, change.index.toInt(), asset, 0, change.amount, mask, keys, lastOutput.receivers, lastOutput.receiversHash, 1, "", OutputState.unspent.name, nowInUtc(), "", "", "", "",
    )
}

fun consolidationOutput(
    hash: String,
    asset: String,
    amount: String,
    mask: String,
    keys: List<String>,
    lastOutput: Output,
): Output {
    if (mask.isEmpty() || keys.isEmpty()) {
        throw Exception("bad mask and keys")
    }
    val outputId = UUID.nameUUIDFromBytes("$hash:0".toByteArray()).toString()
    return Output(
        outputId, hash, 0, asset, 0, amount, mask, keys, lastOutput.receivers, lastOutput.receiversHash, 1, "", "unspent", nowInUtc(), "", "", "", "",
    )
}
