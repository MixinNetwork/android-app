package one.mixin.android.vo.utxo

import kernel.Utxo

data class SignResult(
    val raw: String,
    val change: Utxo?,
)
