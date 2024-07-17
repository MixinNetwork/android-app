package one.mixin.android.ui.wallet;

enum class SnapshotType(val value: Int) {
    All(0),
    Deposit(1),
    Withdrawal(2),
    Transfer(3);


    companion object {
        fun fromInt(value: Int): SnapshotType {
            return entries.find { it.value == value } ?: throw IllegalArgumentException()
        }
    }
}