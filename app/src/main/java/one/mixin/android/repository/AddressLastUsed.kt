package one.mixin.android.repository

import androidx.room3.RoomRawQuery
import one.mixin.android.db.AddressDao
import one.mixin.android.ui.wallet.jsonAddressPredicate
import one.mixin.android.vo.Address
import org.threeten.bp.Instant

// Only privacy wallet withdrawals created before the cutoff count as history; later ones are recorded
// when the transfer is sent from this device.
internal suspend fun backfillLastUsedFromWithdrawals(
    addressDao: AddressDao,
    addresses: List<Address>,
    cutoff: Long,
) {
    val cutoffText = Instant.ofEpochMilli(cutoff).toString()
    addresses.forEach { address ->
        val usedAt = addressDao.queryTexts(historyWithdrawalsQuery(address, cutoffText))
            .map { Instant.parse(it).toEpochMilli() }
            .filter { it < cutoff }
            .maxOrNull() ?: return@forEach
        addressDao.updateLastUsedAt(address.addressId, usedAt)
    }
}

// A withdrawal hash proves the withdrawal went out; local placeholders written before submission carry none.
internal const val HISTORY_WITHDRAWAL_FILTER =
    "s.withdrawal IS NOT NULL AND s.withdrawal != 'null' AND instr(s.withdrawal, '\"withdrawal_hash\":\"\"') = 0 " +
        "AND s.type != 'pending' AND julianday(s.created_at) < julianday(?)"

private fun historyWithdrawalsQuery(address: Address, cutoffText: String): RoomRawQuery {
    val chainId = address.chainId.replace("'", "''")
    val sql = "SELECT s.created_at FROM safe_snapshots s INNER JOIN tokens t ON t.asset_id = s.asset_id " +
        "WHERE t.chain_id = '$chainId' AND $HISTORY_WITHDRAWAL_FILTER " +
        "AND ${jsonAddressPredicate(address.chainId, address.destination, address.tag, "s.withdrawal", "receiver")}"
    return RoomRawQuery(sql) { it.bindText(1, cutoffText) }
}

internal fun missingHistoryTokensQuery(cutoff: Long): RoomRawQuery =
    RoomRawQuery(
        "SELECT DISTINCT s.asset_id FROM safe_snapshots s LEFT JOIN tokens t ON t.asset_id = s.asset_id " +
            "WHERE t.asset_id IS NULL AND $HISTORY_WITHDRAWAL_FILTER",
    ) { it.bindText(1, Instant.ofEpochMilli(cutoff).toString()) }
