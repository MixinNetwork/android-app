package one.mixin.android.repository

import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.Pin
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.service.AddressService
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.AddressDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.vo.Address
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Snapshot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepository
@Inject
constructor(
    private val assetService: AssetService,
    private val assetDao: AssetDao,
    private val snapshotDao: SnapshotDao,
    private val addressDao: AddressDao,
    private val addressService: AddressService
) {

    fun assets() = assetService.assets()

    fun simpleAssetsWithBalance() = assetDao.simpleAssetsWithBalance()

    fun upsert(asset: Asset) {
        val a = assetDao.simpleAsset(asset.assetId)
        if (a != null) {
            asset.hidden = a.hidden
        }
        assetDao.insert(asset)
    }

    fun asset(id: String) = assetService.asset(id)

    fun assetLocal(id: String) = assetDao.simpleAsset(id)

    fun insertAsset(asset: Asset) = assetDao.insert(asset)

    fun insertPendingDeposit(snapshot: List<Snapshot>) = snapshotDao.insertList(snapshot)

    fun snapshots(id: String) = assetService.snapshots(id)

    fun snapshotsFromDb(id: String, type: String? = null, otherType: String? = null) = if (type == null) {
        snapshotDao.snapshots(id)
    } else {
        snapshotDao.snapshotsByType(id, type, otherType)
    }

    fun snapshotLocal(assetId: String, snapshotId: String) = snapshotDao.snapshotLocal(assetId, snapshotId)

    fun insertSnapshot(snapshot: Snapshot) = snapshotDao.insert(snapshot)

    fun getXIN() = assetDao.getXIN()

    fun transfer(transferRequest: TransferRequest) = assetService.transfer(transferRequest)

    fun pay(request: TransferRequest) = assetService.pay(request)

    fun updateHidden(id: String, hidden: Boolean) = assetDao.updateHidden(id, hidden)

    fun hiddenAssetItems() = assetDao.hiddenAssetItems()

    fun addresses(id: String) = addressDao.addresses(id)

    fun withdrawal(withdrawalRequest: WithdrawalRequest) = assetService.withdrawals(withdrawalRequest)

    fun saveAddr(addr: Address) = addressDao.insert(addr)

    fun syncAddr(addressRequest: AddressRequest) = addressService.addresses(addressRequest)

    fun deleteAddr(id: String, pin: String) = addressService.delete(id, Pin(pin))

    fun deleteLocalAddr(id: String) = addressDao.deleteById(id)

    fun assetItems() = assetDao.assetItems()

    fun fuzzySearchAsset(query: String) = assetDao.fuzzySearchAsset(query, query)

    fun assetItem(id: String) = assetDao.assetItem(id)

    fun simpleAssetItem(id: String) = assetDao.simpleAssetItem(id)

    fun assetItemsWithBalance() = assetDao.assetItemsWithBalance()

    fun allSnapshots() = snapshotDao.allSnapshots()

    fun snapshotsByUserId(opponentId: String) = snapshotDao.snapshotsByUserId(opponentId)

    fun pendingDeposits(key: String, asset: String) = assetService.pendingDeposits(key, asset)

    fun clearPendingDepositsByAssetId(assetId: String) = snapshotDao.clearPendingDepositsByAssetId(assetId)
}
