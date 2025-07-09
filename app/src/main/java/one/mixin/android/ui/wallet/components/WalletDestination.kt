package one.mixin.android.ui.wallet.components

sealed class WalletDestination {
    object Privacy : WalletDestination()
    data class Classic(val walletId: String) : WalletDestination()
    data class Import(val walletId: String) : WalletDestination()

    override fun toString(): String {
        return when (this) {
            is Privacy -> "Privacy"
            is Classic -> "Classic_$walletId"
            is Import -> "Import_$walletId"
        }
    }

    companion object {
        fun fromString(value: String?): WalletDestination {
            return when {
                value == null -> Privacy
                value == "Privacy" -> Privacy
                value.startsWith("Classic_") -> Classic(value.removePrefix("Classic_"))
                value.startsWith("Import_") -> Import(value.removePrefix("Import_"))
                else -> Privacy
            }
        }
    }
}
