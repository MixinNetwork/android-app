package one.mixin.android.ui.wallet

/**
 * Web3交易过滤类型
 */
@Suppress("ktlint:standard:enum-entry-name-case", "EnumEntryName")
enum class Web3FilterType(val value: Int) {
    All(0),
    Deposit(1),
    Withdraw(2),
    Swap(3),
    Approve(4),
    Mint(5),
    Execute(6);

    companion object {
        fun fromInt(value: Int): Web3FilterType {
            return entries.find { it.value == value } ?: throw IllegalArgumentException()
        }
    }
}
