package one.mixin.android.vo.utxo

import one.mixin.android.extension.decodeHex
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.Output
import java.util.UUID

class Change(
    val hash: String,
    val index: Int,
    val amount: String,
)

private fun buildChange(hex: String): Change =
    GsonHelper.customGson.fromJson(hex.decodeHex(), Change::class.java)

fun changeToOutput(hex: String, asset: String, createdAt: String): Output {
    val change = buildChange(hex)
    val outputId = UUID.nameUUIDFromBytes("${change.hash}:${change.index}".toByteArray()).toString()
    return Output(outputId, change.hash, change.index, asset, change.amount, "", emptyList(), "", 1, emptyList(), "", "unspent", createdAt, "", "", "", "")
}