package one.mixin.android.ui.wallet

enum class WalletTransferLabelKind {
    ADDRESS,
    COMMON_WALLET,
    CASH,
}

object WalletTransferLabelStyle {
    private const val CASH_ACCOUNT_LABEL = "Cash Account"
    private const val CASH_BACKGROUND = "#AEE666"
    private const val ADDRESS_BACKGROUND = "#66DDAA"
    private const val COMMON_WALLET_BACKGROUND = "#66DDAA"

    fun resolve(
        label: String?,
        toWallet: Boolean = false,
    ): WalletTransferLabelKind? {
        if (label.isNullOrBlank()) return null
        return when {
            label.trim().equals(CASH_ACCOUNT_LABEL, ignoreCase = true) -> WalletTransferLabelKind.CASH
            toWallet -> WalletTransferLabelKind.COMMON_WALLET
            else -> WalletTransferLabelKind.ADDRESS
        }
    }

    fun backgroundColorHex(kind: WalletTransferLabelKind): String =
        when (kind) {
            WalletTransferLabelKind.ADDRESS -> ADDRESS_BACKGROUND
            WalletTransferLabelKind.COMMON_WALLET -> COMMON_WALLET_BACKGROUND
            WalletTransferLabelKind.CASH -> CASH_BACKGROUND
        }

    fun backgroundColorHex(
        label: String?,
        toWallet: Boolean = false,
    ): String = resolve(label, toWallet)?.let(::backgroundColorHex) ?: ADDRESS_BACKGROUND
}
