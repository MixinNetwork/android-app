package one.mixin.android.vo

import android.icu.util.CurrencyAmount

class Utxo(
    val hash: String,
    val amount: String,
    val index: Int = 1,
)
