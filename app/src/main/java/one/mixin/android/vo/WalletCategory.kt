package one.mixin.android.vo

enum class WalletCategory(val value: String) {
    CLASSIC("classic"),
    IMPORTED_MNEMONIC("imported_mnemonic"),
    IMPORTED_PRIVATE_KEY("imported_private_key"),
    WATCH_ADDRESS("watch_address"),
    MIXIN_SAFE("mixin_safe"),
}
