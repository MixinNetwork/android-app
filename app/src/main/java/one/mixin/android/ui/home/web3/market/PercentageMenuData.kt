package one.mixin.android.ui.home.web3.market

enum class PercentageMenuType {
    SEVEN_DAYS,
    TWENTY_FOUR_HOURS
}

data class PercentageMenuData(val type: PercentageMenuType)