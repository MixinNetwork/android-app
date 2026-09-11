package one.mixin.android.repository

import one.mixin.android.api.response.web3.BalanceChange
import one.mixin.android.db.web3.vo.AssetChange

internal data class PendingAssetChanges(
    val senders: List<AssetChange>,
    val receivers: List<AssetChange>,
)

internal fun pendingAssetChanges(balanceChanges: List<BalanceChange>?): PendingAssetChanges {
    val senders = mutableListOf<AssetChange>()
    val receivers = mutableListOf<AssetChange>()
    balanceChanges.orEmpty().forEach { change ->
        val amount = change.amount.toBigDecimalOrNull() ?: return@forEach
        val assetChange = AssetChange(
            assetId = change.assetId,
            amount = amount.abs().toPlainString(),
            from = change.from,
            to = change.to,
        )
        when (amount.signum()) {
            -1 -> senders.add(assetChange)
            1 -> receivers.add(assetChange)
        }
    }
    return PendingAssetChanges(senders, receivers)
}
