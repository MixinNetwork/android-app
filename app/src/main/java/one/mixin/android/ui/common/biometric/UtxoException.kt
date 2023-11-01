package one.mixin.android.ui.common.biometric

import android.content.Context
import one.mixin.android.R
import one.mixin.android.extension.getStackTraceString

const val maxUtxoCount = 256

sealed class UtxoException : Exception() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data object EmptyUtxoException : UtxoException()
data object NotEnoughUtxoException : UtxoException()
data object MaxCountNotEnoughUtxoException : UtxoException()

fun Throwable.isUtxoException(): Boolean = this is EmptyUtxoException || this is NotEnoughUtxoException || this is MaxCountNotEnoughUtxoException

fun Throwable.getUtxoExceptionMsg(context: Context): String {
    val msg = when (this) {
        is EmptyUtxoException -> context.getString(R.string.empty_utxo)
        is NotEnoughUtxoException -> context.getString(R.string.not_enough_utxo)
        is MaxCountNotEnoughUtxoException -> context.getString(R.string.max_count_not_enough_utxo)
        else -> {
            "${context.getString(R.string.gather_utxo)}\n${this.getStackTraceString()}"
        }
    }
    return msg
}
