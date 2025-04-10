package one.mixin.android.ui.wallet

import one.mixin.android.R

@Suppress("ktlint:standard:enum-entry-name-case", "EnumEntryName")
enum class Web3TokenFilterType(val value: Int, val titleRes: Int) {
    ALL(0, R.string.All),
    SEND(1, R.string.Send),
    RECEIVE(2, R.string.Receive),
    APPROVAL(3, R.string.Approval),
    SWAP(4, R.string.Swap),
    PENDING(5, R.string.Pending);

    companion object {
        fun fromInt(value: Int): Web3TokenFilterType {
            return entries.find { it.value == value } ?: ALL
        }
    }
}
