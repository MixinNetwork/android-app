package one.mixin.android.repository

import androidx.paging.DataSource
import javax.inject.Inject
import javax.inject.Singleton
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.Pin
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.service.AddressService
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.AddressDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.AssetDisplayDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.TopAssetDao
import one.mixin.android.vo.Address
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetsExtra
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem

@Singleton
class AssetRepository
@Inject
constructor(
    private val assetService: AssetService,
    private val assetDao: AssetDao,
    private val assetDisplayDao: AssetDisplayDao,
    private val snapshotDao: SnapshotDao,
    private val addressDao: AddressDao,
    private val addressService: AddressService,
    private val hotAssetDao: TopAssetDao
) {

    fun assets() = assetService.assets()

    fun simpleAssetsWithBalance() = assetDao.simpleAssetsWithBalance()

    fun insert(asset: Asset) {
        assetDao.insert(asset)
    }

    fun insertList(asset: List<Asset>) {
        assetDao.insertList(asset)
    }

    fun asset(id: String) = assetService.asset(id)

    suspend fun getAsset(id: String) = assetService.getAsset(id)

    fun assetLocal(id: String) = assetDao.simpleAsset(id)

    fun insertAsset(asset: Asset) = assetDao.insert(asset)

    fun insertPendingDeposit(snapshot: List<Snapshot>) = snapshotDao.insertList(snapshot)

    fun snapshots(id: String) = assetService.snapshots(id)

    fun snapshotsFromDb(
        id: String,
        type: String? = null,
        otherType: String? = null,
        orderByAmount: Boolean = false
    ): DataSource.Factory<Int, SnapshotItem> {
        return if (type == null) {
            if (orderByAmount) {
                snapshotDao.snapshotsOrderByAmount(id)
            } else {
                snapshotDao.snapshots(id)
            }
        } else {
            if (orderByAmount) {
                snapshotDao.snapshotsByTypeOrderByAmount(id, type, otherType)
            } else {
                snapshotDao.snapshotsByType(id, type, otherType)
            }
        }
    }

    fun snapshotLocal(assetId: String, snapshotId: String) = snapshotDao.snapshotLocal(assetId, snapshotId)

    fun insertSnapshot(snapshot: Snapshot) = snapshotDao.insert(snapshot)

    fun getXIN() = assetDao.getXIN()

    fun transfer(transferRequest: TransferRequest) = assetService.transfer(transferRequest)

    fun pay(request: TransferRequest) = assetService.pay(request)

    fun updateHidden(id: String, hidden: Boolean) = assetDisplayDao.insert(AssetsExtra(id, hidden))

    fun hiddenAssetItems() = assetDao.hiddenAssetItems()

    fun addresses(id: String) = addressDao.addresses(id)

    fun observeAddress(addressId: String) = addressDao.observeById(addressId)

    fun withdrawal(withdrawalRequest: WithdrawalRequest) = assetService.withdrawals(withdrawalRequest)

    fun saveAddr(addr: Address) = addressDao.insert(addr)

    fun syncAddr(addressRequest: AddressRequest) = addressService.addresses(addressRequest)

    fun deleteAddr(id: String, pin: String) = addressService.delete(id, Pin(pin))

    fun deleteLocalAddr(id: String) = addressDao.deleteById(id)

    fun assetItems() = assetDao.assetItemsNotHidden()

    suspend fun fuzzySearchAsset(query: String) = assetDao.fuzzySearchAsset(query, query)

    fun assetItem(id: String) = assetDao.assetItem(id)

    fun simpleAssetItem(id: String) = assetDao.simpleAssetItem(id)

    fun assetItemsWithBalance() = assetDao.assetItemsWithBalance()

    fun allSnapshots(
        type: String? = null,
        otherType: String? = null,
        orderByAmount: Boolean = false
    ): DataSource.Factory<Int, SnapshotItem> {
        return if (type == null) {
            if (orderByAmount) {
                snapshotDao.allSnapshotsOrderByAmount()
            } else {
                snapshotDao.allSnapshots()
            }
        } else {
            if (orderByAmount) {
                snapshotDao.allSnapshotsByTypeOrderByAmount(type, otherType)
            } else {
                snapshotDao.allSnapshotsByType(type, otherType)
            }
        }
    }

    fun snapshotsByUserId(opponentId: String) = snapshotDao.snapshotsByUserId(opponentId)

    fun pendingDeposits(asset: String, destination: String, tag: String? = null) = assetService.pendingDeposits(asset, destination, tag)

    fun clearPendingDepositsByAssetId(assetId: String) = snapshotDao.clearPendingDepositsByAssetId(assetId)

    suspend fun queryAssets(query: String) = assetService.queryAssets(query)

    suspend fun getIconUrl(id: String) = assetDao.getIconUrl(id)

    fun observeTopAssets() = hotAssetDao.topAssets()

    fun checkExists(id: String) = assetDao.checkExists(id)

    suspend fun findAddressById(addressId: String, assetId: String) = addressDao.findAddressById(addressId, assetId)

    suspend fun findAssetItemById(assetId: String) = assetDao.findAssetItemById(assetId)
}
