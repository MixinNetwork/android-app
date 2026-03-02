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
        displaySymbol = displaySymbol,
        iconUrl = iconUrl,
        tokenSymbol = tokenSymbol
    )
}

fun PerpsPositionHistoryItem.toPositionHistory(): PerpsPositionHistory {
    return PerpsPositionHistory(
        historyId = historyId,
        positionId = positionId,
        productId = productId,
        marketSymbol = marketSymbol,
        side = side,
        quantity = quantity,
        entryPrice = entryPrice,
        closePrice = closePrice,
        realizedPnl = realizedPnl,
        leverage = leverage,
        marginMethod = marginMethod,
        openAt = openAt,
        closedAt = closedAt,
        walletId = walletId
    )
}
