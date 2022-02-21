package one.mixin.android.repository

import android.os.CancellationSignal
import androidx.paging.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.Pin
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.service.AddressService
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.AddressDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.AssetsExtraDao
import one.mixin.android.db.MessageProvider
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.TopAssetDao
import one.mixin.android.db.TraceDao
import one.mixin.android.extension.within6Hours
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.ErrorHandler.Companion.NOT_FOUND
import one.mixin.android.vo.Address
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.AssetsExtra
import one.mixin.android.vo.PriceAndChange
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.Trace
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepository
@Inject
constructor(
    private val appDatabase: MixinDatabase,
    private val assetService: AssetService,
    private val assetDao: AssetDao,
    private val assetsExtraDao: AssetsExtraDao,
    private val snapshotDao: SnapshotDao,
    private val addressDao: AddressDao,
    private val addressService: AddressService,
    private val hotAssetDao: TopAssetDao,
    private val traceDao: TraceDao
) {

    fun assets() = assetService.assets()

    suspend fun simpleAssetsWithBalance() = assetDao.simpleAssetsWithBalance()

    fun insert(asset: Asset) {
        assetDao.insert(asset)
    }

    fun insertList(asset: List<Asset>) {
        assetDao.insertList(asset)
    }

    suspend fun asset(id: String) = assetService.getAssetByIdSuspend(id)

    suspend fun findOrSyncAsset(assetId: String): AssetItem? {
        var assetItem = assetDao.findAssetItemById(assetId)
        if (assetItem != null) return assetItem

        assetItem = syncAsset(assetId)
        if (assetItem != null && assetItem.chainId != assetItem.assetId && simpleAsset(assetItem.chainId) == null) {
            val chain = syncAsset(assetItem.chainId)
            assetItem.chainIconUrl = chain?.chainIconUrl
            assetItem.chainSymbol = chain?.chainSymbol
            assetItem.chainName = chain?.chainName
            assetItem.chainPriceUsd = chain?.chainPriceUsd
        }
        return assetItem
    }

    private suspend fun syncAsset(assetId: String): AssetItem? {
        return handleMixinResponse(
            invokeNetwork = {
                assetService.getAssetByIdSuspend(assetId)
            },
            switchContext = Dispatchers.IO,
            successBlock = { chain ->
                chain.data?.let { c -> insert(c) }
                return@handleMixinResponse assetDao.findAssetItemById(assetId)
            }
        )
    }

    suspend fun simpleAsset(id: String) = assetDao.simpleAsset(id)

    fun insertAsset(asset: Asset) = assetDao.insert(asset)

    suspend fun insertPendingDeposit(snapshot: List<Snapshot>) = snapshotDao.insertListSuspend(snapshot)

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

    suspend fun snapshotLocal(assetId: String, snapshotId: String) = snapshotDao.snapshotLocal(assetId, snapshotId)

    fun insertSnapshot(snapshot: Snapshot) = snapshotDao.insert(snapshot)

    fun getXIN() = assetDao.getXIN()

    suspend fun transfer(transferRequest: TransferRequest) = assetService.transfer(transferRequest)

    suspend fun paySuspend(request: TransferRequest) = assetService.paySuspend(request)

    suspend fun updateHidden(id: String, hidden: Boolean) = assetsExtraDao.insertSuspend(AssetsExtra(id, hidden))

    fun hiddenAssetItems() = assetDao.hiddenAssetItems()

    fun addresses(id: String) = addressDao.addresses(id)

    fun observeAddress(addressId: String) = addressDao.observeById(addressId)

    suspend fun withdrawal(withdrawalRequest: WithdrawalRequest) = assetService.withdrawals(withdrawalRequest)

    fun saveAddr(addr: Address) = addressDao.insert(addr)

    suspend fun syncAddr(addressRequest: AddressRequest) = addressService.addresses(addressRequest)

    suspend fun deleteAddr(id: String, pin: String) = addressService.delete(id, Pin(pin))

    suspend fun deleteLocalAddr(id: String) = addressDao.deleteById(id)

    fun assetItems() = assetDao.assetItemsNotHidden()

    suspend fun fuzzySearchAsset(query: String, cancellationSignal: CancellationSignal) =
        MessageProvider.fuzzySearchAsset(query, query, appDatabase, cancellationSignal)

    suspend fun fuzzySearchAssetIgnoreAmount(query: String) = assetDao.fuzzySearchAssetIgnoreAmount(query, query)

    fun assetItem(id: String) = assetDao.assetItem(id)

    suspend fun simpleAssetItem(id: String) = assetDao.simpleAssetItem(id)

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

    suspend fun pendingDeposits(asset: String, destination: String, tag: String? = null) =
        assetService.pendingDeposits(asset, destination, tag)

    suspend fun clearPendingDepositsByAssetId(assetId: String) = snapshotDao.clearPendingDepositsByAssetId(assetId)

    suspend fun queryAssets(query: String) = assetService.queryAssets(query)

    suspend fun getIconUrl(id: String) = assetDao.getIconUrl(id)

    fun observeTopAssets() = hotAssetDao.topAssets()

    fun checkExists(id: String) = assetDao.checkExists(id)

    suspend fun findAddressById(addressId: String, assetId: String) = addressDao.findAddressById(addressId, assetId)

    suspend fun refreshAndGetAddress(addressId: String, assetId: String): Pair<Address?, Boolean> {
        var result: Address? = null
        var notExists = false
        handleMixinResponse(
            invokeNetwork = {
                addressService.address(addressId)
            },
            successBlock = { response ->
                response.data?.let {
                    addressDao.insert(it)
                    result = addressDao.findAddressById(addressId, assetId)
                }
            },
            failureBlock = {
                if (it.errorCode == NOT_FOUND) {
                    notExists = true
                }
                return@handleMixinResponse false
            }
        )
        return Pair(result, notExists)
    }

    suspend fun findAssetItemById(assetId: String) = assetDao.findAssetItemById(assetId)

    suspend fun findAssetsByIds(assetIds: List<String>) = assetDao.suspendFindAssetsByIds(assetIds)

    suspend fun findSnapshotById(snapshotId: String) = snapshotDao.findSnapshotById(snapshotId)

    suspend fun findSnapshotByTraceId(traceId: String) = snapshotDao.findSnapshotByTraceId(traceId)

    suspend fun refreshAndGetSnapshot(snapshotId: String): SnapshotItem? {
        var result: SnapshotItem? = null
        handleMixinResponse(
            invokeNetwork = {
                assetService.getSnapshotById(snapshotId)
            },
            successBlock = { response ->
                response.data?.let {
                    snapshotDao.insert(it)
                    result = snapshotDao.findSnapshotById(snapshotId)
                }
            }
        )
        return result
    }

    suspend fun getSnapshots(assetId: String, offset: String?, limit: Int, opponent: String?, destination: String?, tag: String?) =
        assetService.getSnapshots(assetId, offset, limit, opponent, destination, tag)

    suspend fun insertTrace(trace: Trace) = traceDao.insertSuspend(trace)

    suspend fun suspendFindTraceById(traceId: String): Trace? = traceDao.suspendFindTraceById(traceId)

    suspend fun getTrace(traceId: String) = assetService.getTrace(traceId)

    suspend fun findLatestTrace(opponentId: String?, destination: String?, tag: String?, amount: String, assetId: String): Pair<Trace?, Boolean> {
        val trace = traceDao.suspendFindTrace(opponentId, destination, tag, amount, assetId) ?: return Pair(null, false)

        val with6hours = trace.createdAt.within6Hours()
        if (!with6hours) {
            return Pair(null, false)
        }

        if (trace.snapshotId.isNullOrBlank()) {
            val response = try {
                withContext(Dispatchers.IO) {
                    assetService.getTrace(trace.traceId)
                }
            } catch (t: Throwable) {
                ErrorHandler.handleError(t)
                return Pair(null, true)
            }
            return if (response.isSuccess) {
                trace.snapshotId = response.data?.snapshotId
                traceDao.insertSuspend(trace)
                Pair(trace, false)
            } else {
                if (response.errorCode == NOT_FOUND) {
                    Pair(null, false)
                } else {
                    if (response.errorCode == FORBIDDEN) {
                        traceDao.suspendDeleteById(trace.traceId)
                    }
                    ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
                    Pair(null, true)
                }
            }
        } else {
            return Pair(trace, false)
        }
    }

    suspend fun deletePreviousTraces() = traceDao.deletePreviousTraces()

    suspend fun suspendDeleteTraceById(traceId: String) = traceDao.suspendDeleteById(traceId)

    suspend fun ticker(assetId: String, offset: String?) = assetService.ticker(assetId, offset)

    suspend fun findSnapshotByTransactionHashList(assetId: String, hashList: List<String>): List<String> =
        snapshotDao.findSnapshotIdsByTransactionHashList(assetId, hashList)

    suspend fun suspendUpdatePrices(priceAndChange: List<PriceAndChange>) =
        assetDao.suspendUpdatePrices(priceAndChange)

    suspend fun findTotalUSDBalance(): Int = assetDao.findTotalUSDBalance() ?: 0

    suspend fun findAllAssetIdSuspend() = assetDao.findAllAssetIdSuspend()
}
