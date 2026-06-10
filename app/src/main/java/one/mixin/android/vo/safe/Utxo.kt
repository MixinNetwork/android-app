package one.mixin.android.vo.safe

data class Utxo(
    val hash: String,
    val amount: String,
    val index: Int = 1,
)
