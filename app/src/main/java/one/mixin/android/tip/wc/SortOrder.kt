package one.mixin.android.tip.wc


enum class SortOrder(val value: Int) {
    Recent(0),
    Alphabetical(1),
    Oldest(2),
    Value(3),
    Amount(4);

    companion object {
        fun fromInt(value: Int): SortOrder {
            return entries.find { it.value == value } ?: throw IllegalArgumentException()
        }
    }
}