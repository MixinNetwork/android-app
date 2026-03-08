package one.mixin.android.api.response.perps

fun PerpsPositionItem.toPosition(): PerpsPosition {
    return PerpsPosition(
        positionId = positionId,
        productId = productId,
        side = side,
        quantity = quantity,
        entryPrice = entryPrice,
        leverage = leverage,
        settleAssetId = settleAssetId,
        botId = botId,
        margin = margin,
        state = state,
        markPrice = markPrice,
        unrealizedPnl = unrealizedPnl,
        roe = roe,
        walletId = walletId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

