package one.mixin.android.ui.wallet.home

object WalletHomeBuilder {
    fun build(
        walletType: WalletHomeType,
        hasAssetValue: Boolean,
        showBanner: Boolean,
        showReferral: Boolean,
        hasPositions: Boolean,
        hasTopMovers: Boolean,
        hasTransactions: Boolean,
        hasImportKeyAction: Boolean = false,
        hasPendingIndicator: Boolean = false,
        isLoading: Boolean = false,
    ): List<WalletHomeCardType> {
        if (isLoading && !hasAssetValue) return emptyList()

        val cards = mutableListOf<WalletHomeCardType>()

        cards += if (hasAssetValue || hasImportKeyAction || hasPendingIndicator) WalletHomeCardType.BALANCE else WalletHomeCardType.EMPTY_GUIDE
        if (showBanner) cards += WalletHomeCardType.BANNER
        val showTopMovers = walletType == WalletHomeType.PRIVACY && hasTopMovers
        if (walletType == WalletHomeType.PRIVACY && hasPositions) cards += WalletHomeCardType.POSITIONS
        if (!hasPositions && showTopMovers) cards += WalletHomeCardType.TOP_MOVERS

        cards += WalletHomeCardType.TOKENS

        if (hasTransactions) cards += WalletHomeCardType.TRANSACTIONS

        if (hasPositions && showTopMovers) {
            cards += WalletHomeCardType.TOP_MOVERS
        }

        if (showReferral) cards += WalletHomeCardType.REFERRAL
        cards += WalletHomeCardType.SUPPORT

        return cards
    }
}
