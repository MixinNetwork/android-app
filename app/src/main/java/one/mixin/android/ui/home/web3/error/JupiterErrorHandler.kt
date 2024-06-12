package one.mixin.android.ui.home.web3.error

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

class JupiterErrorHandler(
    override val log: String,
) : Handler(log) {
    override fun parse(chain: Chain): String? {
        return parseInternal()?.toString(chain.ctx) ?: chain.process()
    }

    internal fun parseInternal(): JupiterError? {
        val regex = Regex("""Program log: AnchorError occurred\. Error Code: (?<code>\w+)\. Error Number: (?<number>\d+)\. Error Message: (?<message>.+)\.""")
        val matchResult = regex.find(log)
        if (matchResult != null) {
            val (code, number, message) = matchResult.destructured
            return JupiterError(code, number.toInt(), message)
        }
        return null
    }
}
