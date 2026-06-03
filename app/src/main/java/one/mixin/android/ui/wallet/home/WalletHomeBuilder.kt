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
        isLoading: Boolean = false,
    ): List<WalletHomeCardType> {
        if (isLoading && !hasAssetValue) return emptyList()
        val cards = mutableListOf<WalletHomeCardType>()

        cards += if (hasAssetValue) WalletHomeCardType.BALANCE else WalletHomeCardType.EMPTY_GUIDE
        if (showBanner) cards += WalletHomeCardType.BANNER
        if (walletType == WalletHomeType.PRIVACY && hasPositions) cards += WalletHomeCardType.POSITIONS

        cards += WalletHomeCardType.TOKENS

        if (walletType == WalletHomeType.PRIVACY && !hasPositions && hasTopMovers) {
            cards += WalletHomeCardType.TOP_MOVERS
        }
        if (hasTransactions) cards += WalletHomeCardType.TRANSACTIONS

        if (showReferral) cards += WalletHomeCardType.REFERRAL
        cards += WalletHomeCardType.SUPPORT

        return cards
    }
}
