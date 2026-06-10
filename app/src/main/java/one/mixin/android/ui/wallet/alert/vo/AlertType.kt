package one.mixin.android.ui.wallet.alert.vo

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import one.mixin.android.R

enum class AlertType(val value: String) {
    @SerializedName("price_reached")
    PRICE_REACHED("price_reached"),

    @SerializedName("price_increased")
    PRICE_INCREASED("price_increased"),

    @SerializedName("price_decreased")
    PRICE_DECREASED("price_decreased"),

    @SerializedName("percentage_increased")
    PERCENTAGE_INCREASED("percentage_increased"),

    @SerializedName("percentage_decreased")
    PERCENTAGE_DECREASED("percentage_decreased");

    @DrawableRes
    fun getIconResId(): Int {
        return when (this) {
            PRICE_REACHED -> R.drawable.ic_reached
            PRICE_INCREASED -> R.drawable.ic_increased
            PRICE_DECREASED -> R.drawable.ic_decreased
            PERCENTAGE_INCREASED -> R.drawable.ic_increased
            PERCENTAGE_DECREASED -> R.drawable.ic_decreased
        }
    }

    @StringRes
    fun getTitleResId(): Int {
        return when (this) {
            PRICE_REACHED -> R.string.alert_type_price_reached
            PRICE_INCREASED -> R.string.alert_type_price_increased
            PRICE_DECREASED -> R.string.alert_type_price_decreased
            PERCENTAGE_INCREASED -> R.string.alert_type_percentage_increased
            PERCENTAGE_DECREASED -> R.string.alert_type_percentage_decreased
        }
    }

    @StringRes
    fun getSubTitleResId(): Int {
        return when (this) {
            PRICE_REACHED -> R.string.alert_type_price_reached_description
            PRICE_INCREASED -> R.string.alert_type_price_increased_description
            PRICE_DECREASED -> R.string.alert_type_price_decreased_description
            PERCENTAGE_INCREASED -> R.string.alert_type_percentage_increased_description
            PERCENTAGE_DECREASED -> R.string.alert_type_percentage_decreased_description
        }
    }
}
