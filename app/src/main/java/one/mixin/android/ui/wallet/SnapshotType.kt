package one.mixin.android.ui.wallet

import one.mixin.android.ui.wallet.SnapshotType.entries

@Suppress("ktlint:standard:enum-entry-name-case", "EnumEntryName")
enum class SnapshotType(val value: Int) {
    all(0),
    deposit(1),
    withdrawal(2),
    snapshot(3);

    companion object {
        fun fromInt(value: Int): SnapshotType {
            return entries.find { it.value == value } ?: throw IllegalArgumentException()
        }
    }
}
