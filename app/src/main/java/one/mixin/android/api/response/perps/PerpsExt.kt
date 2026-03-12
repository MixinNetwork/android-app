package one.mixin.android.api.response.perps

fun PerpsPositionItem.toPosition(): PerpsPosition {
    return PerpsPosition(
        positionId = positionId,
        marketId = marketId,
        side = side,
        quantity = quantity,
        entryPrice = entryPrice,
        leverage = leverage,
        settleAssetId = settleAssetId ?: "",
        botId = botId ?: "",
        margin = margin ?: "0",
        openPayAmount = openPayAmount ?: "0",
        openPayAssetId = openPayAssetId ?: "",
        state = state ?: "",
        markPrice = markPrice ?: "0",
        unrealizedPnl = unrealizedPnl ?: "0",
        roe = roe ?: "0",
        walletId = walletId ?: "",
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: "",
    )
}
