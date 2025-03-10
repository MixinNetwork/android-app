package one.mixin.android.ui.wallet

@Suppress("ktlint:standard:enum-entry-name-case", "EnumEntryName")
enum class SnapshotType(val value: Int) {
    all(0),
    deposit(1),
    withdrawal(2),
    snapshot(3),
    pending(4);

    companion object {
        fun fromInt(value: Int): SnapshotType {
            return entries.find { it.value == value } ?: throw IllegalArgumentException()
        }
    }
}
