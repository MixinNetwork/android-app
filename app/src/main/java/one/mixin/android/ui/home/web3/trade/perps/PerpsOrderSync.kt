package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.api.response.perps.PerpsOrder

internal suspend fun syncPerpsOrderPages(
    initialOffset: String?,
    limit: Int,
    fetch: suspend (String?) -> List<PerpsOrder>,
    save: suspend (List<PerpsOrder>) -> Unit,
) {
    require(limit > 0)
    var offset = initialOffset
    while (true) {
        val orders = fetch(offset)
        save(orders)
        if (orders.size < limit) return
        val nextOffset = orders.last().updatedAt
        check(nextOffset.isNotBlank() && nextOffset != offset) { "Perps order cursor did not advance" }
        offset = nextOffset
    }
}
