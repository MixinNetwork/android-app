package one.mixin.android.ui.wallet.home

object WalletHomeSection {
    const val PREVIEW_LIMIT = 3
    const val MORE_DETECTION_LIMIT = PREVIEW_LIMIT + 1

    fun previewCount(totalCount: Int): Int = totalCount.coerceIn(0, PREVIEW_LIMIT)

    fun hasMore(totalCount: Int): Boolean = totalCount > PREVIEW_LIMIT
}
