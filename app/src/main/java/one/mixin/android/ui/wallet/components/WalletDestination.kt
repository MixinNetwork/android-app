package one.mixin.android.ui.wallet.components

sealed class WalletDestination {
    object Privacy : WalletDestination()
    data class Classic(val walletId: String) : WalletDestination()
    data class Private(val walletId: String) : WalletDestination()

    override fun toString(): String {
        return when (this) {
            is Privacy -> "Privacy"
            is Classic -> "Classic(walletId=$walletId)"
            is Private -> "Private(walletId=$walletId)"
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
                value.startsWith("Private(walletId=") && value.endsWith(")") -> {
                    val walletId = value.removePrefix("Private(walletId=").removeSuffix(")")
                    Private(walletId)
                }
                else -> Privacy
            }
        }
    }
}
