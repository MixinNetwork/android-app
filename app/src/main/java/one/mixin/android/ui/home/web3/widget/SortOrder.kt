package one.mixin.android.ui.home.web3.widget

enum class MarketSort(val value: Int) {
    RANK_ASCENDING(0),
    RANK_DESCENDING(1),
    PRICE_ASCENDING(2),
    PRICE_DESCENDING(3),
    PERCENTAGE_ASCENDING(4),
    PERCENTAGE_DESCENDING(5);

    companion object {
        fun fromValue(value: Int): MarketSort {
            return entries.firstOrNull { it.value == value } ?: RANK_ASCENDING
        }
    }
}