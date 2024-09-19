package one.mixin.android.ui.wallet.alert.vo

import one.mixin.android.R

enum class AlertType(val value: String) {
    PRICE_REACHED("price_reached"),
    PRICE_INCREASED("price_increased"),
    PRICE_DECREASED("price_decreased"),
    PERCENTAGE_INCREASED("percentage_increased"),
    PERCENTAGE_DECREASED("percentage_decreased");

    fun getStringResId(): Int {
        return when (this) {
            PRICE_REACHED -> R.string.alert_type_price_reached
            PRICE_INCREASED -> R.string.alert_type_price_increased
            PRICE_DECREASED -> R.string.alert_type_price_decreased
            PERCENTAGE_INCREASED -> R.string.alert_type_percentage_increased
            PERCENTAGE_DECREASED -> R.string.alert_type_percentage_decreased
        }
    }
}
