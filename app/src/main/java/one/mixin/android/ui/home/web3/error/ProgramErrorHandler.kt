package one.mixin.android.ui.home.web3.error

import android.content.Context
import one.mixin.android.R

data class ProgramError(val message: String) {

    fun toString(ctx: Context): String {
        return when (message) {
            "insufficient funds" -> ctx.getString(R.string.insufficient_balance)
            else -> "Error Message: $message"
        }
    }
}

class ProgramErrorHandler(
    override val log: String,
) : Handler(log) {
    override fun parse(chain: Chain): String? {
        return parseInternal()?.toString(chain.ctx) ?:  chain.process()
    }

    internal fun parseInternal(): ProgramError? {
        val regex = Regex("""Program log: Error: (?<message>.*?)(?="|$)""")
        val matchResult = regex.find(log)
        if (matchResult != null) {
            val (message) = matchResult.destructured
            return ProgramError(message)
        }
        return null
    }
}