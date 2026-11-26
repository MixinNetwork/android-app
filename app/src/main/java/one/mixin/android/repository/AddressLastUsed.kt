package one.mixin.android.repository

import one.mixin.android.db.AddressDao
import one.mixin.android.db.SafeSnapshotDao
import one.mixin.android.ui.wallet.FilterParams
import one.mixin.android.ui.wallet.SnapshotType
import one.mixin.android.vo.Address
import org.threeten.bp.Instant

// Only privacy wallet withdrawals created before the cutoff count as history; later ones are recorded
// when the transfer is sent from this device.
internal suspend fun backfillLastUsedFromWithdrawals(
    addressDao: AddressDao,
    safeSnapshotDao: SafeSnapshotDao,
    addresses: List<Address>,
    cutoff: Long,
) {
    addresses.forEach { address ->
        val usedAt = safeSnapshotDao.snapshotItems(FilterParams(type = SnapshotType.withdrawal, address = address).buildQuery())
            .firstNotNullOfOrNull { snapshot -> Instant.parse(snapshot.createdAt).toEpochMilli().takeIf { it < cutoff } }
            ?: return@forEach
        addressDao.updateLastUsedAt(address.addressId, usedAt)
    }
}
