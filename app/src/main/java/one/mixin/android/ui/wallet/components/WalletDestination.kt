package one.mixin.android.ui.wallet.components

sealed class WalletDestination {
    object Privacy : WalletDestination()
    data class Classic(val walletId: String) : WalletDestination()
    data class Import(val walletId: String, val category: String) : WalletDestination()
}


