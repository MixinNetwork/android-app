package one.mixin.android.ui.wallet.components

sealed class WalletDestination {
    object Privacy : WalletDestination()
    data class Classic(val walletId: String) : WalletDestination()
    data class Import(val walletId: String) : WalletDestination()

    override fun toString(): String {
        return when (this) {
            is Privacy -> "Privacy"
            is Classic -> "Classic(walletId=$walletId)"
            is Import -> "Import(walletId=$walletId)"
        }
    }

    companion object {
        fun fromString(value: String?): WalletDestination {
            return when {
                value == null -> Privacy
                value == "Privacy" -> Privacy
                value.startsWith("Classic(walletId=") && value.endsWith(")") -> {
                    val walletId = value.removePrefix("Classic(walletId=").removeSuffix(")")
                    Classic(walletId)
                }
                value.startsWith("Import(walletId=") && value.endsWith(")") -> {
                    val walletId = value.removePrefix("Import(walletId=").removeSuffix(")")
                    Import(walletId)
                }
                else -> Privacy
            }
        }
    }
}
