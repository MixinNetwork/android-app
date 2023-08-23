package one.mixin.android.ui.wallet.fiatmoney

object AmountUtil {
    private val full_currency = listOf(
        "JPY",
        "KRW",
    )

    fun toAmount(value: String, currency: String): Int? {
        val v = value.toFloatOrNull() ?: return null
        if (currency in full_currency) {
            return v.toInt()
        } else {
            return (v * 100).toInt()
        }
    }

    fun realAmount(value: Int, currency: String): String {
        if (currency in full_currency) {
            return value.toString()
        } else {
            return (value / 100f).toString()
        }
    }
}
