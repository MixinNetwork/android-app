package one.mixin.android.ui.home.web3.widget

import one.mixin.android.ui.home.web3.widget.MarketSort.entries

enum class MarketSort(val value: Int) {
    RANK_ASCENDING(0),
    RANK_DESCENDING(1),
    PRICE_ASCENDING(2),
    PRICE_DESCENDING(3),
    SEVEN_DAYS_PERCENTAGE_ASCENDING(4),
    SEVEN_DAYS_PERCENTAGE_DESCENDING(5),
    TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING(6),
    TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING(7);

    companion object {
        fun fromValue(value: Int): MarketSort {
            return entries.firstOrNull { it.value == value } ?: RANK_ASCENDING
        }
    }
}