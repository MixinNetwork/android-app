package one.mixin.android.ui.home.web3.error

data class RaydiumError(
    val code: String,
    val number: Int,
    val message: String,
) {
    companion object {
        const val invalidFirstTickArrayAccount = 6028
    }

    override fun toString(): String {
        return "Error Code: $code, Error number: $number, Error Message: $message"
    }
}

class RaydiumErrorHandler(override val log: String) : Handler(log){
    override fun parse(chain: Chain): String? {
        return parseInternal()?.toString() ?: chain.process()
    }

    internal fun parseInternal(): RaydiumError? {
        val regex = Regex("""AnchorError thrown in (?<source>.+) Error Code: (?<code>\w+)\. Error Number: (?<number>\d+)\. Error Message: (?<message>.+)\.""")
        val matchResult = regex.find(log)
        if (matchResult != null) {
            val (_, code, number, message) = matchResult.destructured
            return RaydiumError(code, number.toInt(), message)
        }
        return null
    }
}