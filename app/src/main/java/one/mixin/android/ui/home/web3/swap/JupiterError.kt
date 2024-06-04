package one.mixin.android.ui.home.web3.swap

import android.content.Context
import one.mixin.android.R

data class JupiterError(
    val code: String,
    val number: Int,
    val message: String,
) {
    companion object {
        const val slippageToleranceExceeded = 6001
    }

    fun toString(ctx: Context): String {
        return when (number) {
            slippageToleranceExceeded -> ctx.getString(R.string.Slippage_tolerance_exceeded)
            else -> "Error Code: $code, Error number: $number, Error Message: $message"
        }
    }
}

fun parseJupiterError(raw: String): JupiterError? {
    val regex = Regex("""Error Code: (?<code>\w+)\. Error Number: (?<number>\d+)\. Error Message: (?<message>.+)\.""")
    val matchResult = regex.find(raw)
    if (matchResult != null) {
        val (code, number, message) = matchResult.destructured
        return JupiterError(code, number.toInt(), message)
    }
    return null
}
