package one.mixin.android.repository

import android.os.CancellationSignal
import androidx.datastore.core.DataStore
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.DepositEntryRequest
import one.mixin.android.api.request.Pin
import one.mixin.android.api.request.RouteInstrumentRequest
import one.mixin.android.api.request.RouteSessionRequest
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.request.RouteTokenRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.response.RoutePaymentResponse
import one.mixin.android.api.response.RouteSessionResponse
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.api.service.AddressService
import one.mixin.android.api.service.AssetService
import one.mixin.android.api.service.RouteService
import one.mixin.android.api.service.UtxoService
import one.mixin.android.db.AddressDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.AssetsExtraDao
import one.mixin.android.db.ChainDao
import one.mixin.android.db.DepositDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.TokenDao
import one.mixin.android.db.TopAssetDao
import one.mixin.android.db.TraceDao
import one.mixin.android.db.provider.DataProvider
import one.mixin.android.extension.getRFC3339Nano
import one.mixin.android.extension.within6Hours
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncOutputJob
import one.mixin.android.session.Session
import one.mixin.android.session.buildHashMembers
import one.mixin.android.ui.wallet.adapter.SnapshotsMediator
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.ErrorHandler.Companion.NOT_FOUND
import one.mixin.android.vo.Address
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.AssetsExtra
import one.mixin.android.vo.Card
import one.mixin.android.vo.Deposit
import one.mixin.android.vo.PriceAndChange
import one.mixin.android.vo.SafeBox
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.Token
import one.mixin.android.vo.Trace
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.route.RoutePaymentRequest
import one.mixin.android.vo.sumsub.ProfileResponse
import one.mixin.android.vo.sumsub.RouteTokenResponse
import one.mixin.android.vo.toAssetItem
import one.mixin.android.vo.toPriceAndChange
import retrofit2.Call
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepository
@Inject
constructor(
    private val appDatabase: MixinDatabase,
    private val assetService: AssetService,
    private val utxoService: UtxoService,
    private val routeService: RouteService,
    private val assetDao: AssetDao,
    private val tokeDao: TokenDao,
    private val assetsExtraDao: AssetsExtraDao,
    private val snapshotDao: SnapshotDao,
    private val addressDao: AddressDao,
    private val addressService: AddressService,
    private val hotAssetDao: TopAssetDao,
    private val traceDao: TraceDao,
    private val chainDao: ChainDao,
    private val depositDao: DepositDao,
    private val jobManager: MixinJobManager,
    private val safeBox: DataStore<SafeBox>,
) {

    fun assets() = assetService.assets()

    suspend fun simpleAssetsWithBalance() = assetDao.simpleAssetsWithBalance()

    fun insert(asset: Token) {
        tokeDao.insert(asset)
    }

    fun insertList(asset: List<Token>) {
        tokeDao.insertList(asset)
    }

    suspend fun asset(id: String) = assetService.getAssetByIdSuspend(id)

    suspend fun getAssetPrecisionById(id: String) = assetService.getAssetPrecisionById(id)

    suspend fun findOrSyncAsset(assetId: String): AssetItem? {
        var assetItem = assetDao.findAssetItemById(assetId)
        if (assetItem != null && assetItem.getDestination().isNotBlank()) {
            return assetItem
        } else if (assetItem != null) {
            val userId = requireNotNull(Session.getAccountId())
            handleMixinResponse(
                invokeNetwork = {
                    utxoService.createDeposit(
                        DepositEntryRequest(1, listOf(userId), assetItem!!.chainId)
                    )
                },
                successBlock = { resp ->
                    resp.data?.let { list ->
                        depositDao.insertList(list)
                    }
                },
            ) ?: return null
        }

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

    suspend fun createDeposit(chinaId: String, assetId: String): MixinResponse<List<Deposit>> {
        val userId = requireNotNull(Session.getAccountId())
        return utxoService.createDeposit(
            DepositEntryRequest(1, listOf(userId), chinaId)
        )
    }

    suspend fun syncAsset(assetId: String): AssetItem? {
        val asset: Token = handleMixinResponse(
            invokeNetwork = {
                assetService.getAssetByIdSuspend(assetId)
            },
            successBlock = { resp ->
                resp.data?.let { a ->
                    insert(a)
                    a
                }
            },
        ) ?: return null

        val exists = chainDao.checkExistsById(asset.chainId)
        if (exists == null) {
            handleMixinResponse(
                invokeNetwork = {
                    assetService.getChainById(asset.chainId)
                },
                successBlock = { resp ->
                    resp.data?.let { c ->
                        chainDao.upsertSuspend(c)
                    }
                },
            )
        }

        return assetDao.findAssetItemById(assetId)
    }

    private suspend fun simpleAsset(id: String) = assetDao.simpleAsset(id)

    suspend fun insertPendingDeposit(snapshot: List<Snapshot>) = snapshotDao.insertListSuspend(snapshot)

    @ExperimentalPagingApi
    fun snapshots(
        assetId: String,
        type: String? = null,
        otherType: String? = null,
        orderByAmount: Boolean = false,
    ): LiveData<PagingData<SnapshotItem>> =
        Pager(
            config = PagingConfig(
                pageSize = Constants.PAGE_SIZE,
                enablePlaceholders = true,
            ),
            pagingSourceFactory = {
                if (type == null) {
                    if (orderByAmount) {
                        snapshotDao.snapshotsOrderByAmountPaging(assetId)
                    } else {
                        snapshotDao.snapshotsPaging(assetId)
                    }
                } else {
                    if (orderByAmount) {
                        snapshotDao.snapshotsByTypeOrderByAmountPaging(assetId, type, otherType)
                    } else {
                        snapshotDao.snapshotsByTypePaging(assetId, type, otherType)
                    }
                }
            },
            remoteMediator = SnapshotsMediator(
                assetService,
                snapshotDao,
                assetDao,
                jobManager,
                assetId
            ),
        ).liveData

    fun snapshotsFromDb(
        id: String,
        type: String? = null,
        otherType: String? = null,
        orderByAmount: Boolean = false,
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

    suspend fun snapshotLocal(assetId: String, snapshotId: String) =
        snapshotDao.snapshotLocal(assetId, snapshotId)

    fun insertSnapshot(snapshot: Snapshot) = snapshotDao.insert(snapshot)

    fun getXIN() = assetDao.getXIN()

    suspend fun transfer(transferRequest: TransferRequest) = assetService.transfer(transferRequest)

    suspend fun paySuspend(request: TransferRequest) = assetService.paySuspend(request)

    suspend fun updateHidden(id: String, hidden: Boolean) {
        appDatabase.withTransaction {
            val assetsExtra = assetsExtraDao.findByAssetId(id)
            if (assetsExtra != null) {
                assetsExtraDao.updateHiddenByAssetId(id, hidden)
            } else {
                assetsExtraDao.insertSuspend(
                    AssetsExtra(
                        id,
                        assetIdToAsset(id),
                        hidden,
                        null,
                        null
                    )
                )
            }
        }
    }

    fun hiddenAssetItems() = assetDao.hiddenAssetItems()

    fun addresses(id: String) = addressDao.addresses(id)

    fun observeAddress(addressId: String) = addressDao.observeById(addressId)

    suspend fun withdrawal(withdrawalRequest: WithdrawalRequest) =
        assetService.withdrawals(withdrawalRequest)

    fun saveAddr(addr: Address) = addressDao.insert(addr)

    suspend fun syncAddr(addressRequest: AddressRequest) = addressService.addresses(addressRequest)

    suspend fun deleteAddr(id: String, pin: String) = addressService.delete(id, Pin(pin))

    suspend fun deleteLocalAddr(id: String) = addressDao.deleteById(id)

    fun assetItemsNotHidden() = assetDao.assetItemsNotHidden()

    fun assetItems() = assetDao.assetItems()

    fun assetItems(assetIds: List<String>) = assetDao.assetItems(assetIds)

    suspend fun fuzzySearchAsset(query: String, cancellationSignal: CancellationSignal) =
        DataProvider.fuzzySearchAsset(query, query, appDatabase, cancellationSignal)

    suspend fun fuzzySearchAssetIgnoreAmount(query: String) =
        assetDao.fuzzySearchAssetIgnoreAmount(query, query)

    fun assetItem(id: String) = assetDao.assetItem(id)

    suspend fun simpleAssetItem(id: String) = assetDao.simpleAssetItem(id)

    fun assetItemsWithBalance() = assetDao.assetItemsWithBalance()

    fun allSnapshots(
        type: String? = null,
        otherType: String? = null,
        orderByAmount: Boolean = false,
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

    suspend fun clearPendingDepositsByAssetId(assetId: String) =
        snapshotDao.clearPendingDepositsByAssetId(assetId)

    suspend fun queryAsset(query: String): List<AssetItem> {
        val response = try {
            queryAssets(query)
        } catch (t: Throwable) {
            ErrorHandler.handleError(t)
            return emptyList()
        }
        if (response.isSuccess) {
            val assetList = response.data as List<Token>
            if (assetList.isEmpty()) {
                return emptyList()
            }
            val assetItemList = arrayListOf<AssetItem>()
            assetList.mapTo(assetItemList) { asset ->
                var chainIconUrl = getIconUrl(asset.chainId)
                if (chainIconUrl == null) {
                    chainIconUrl = fetchAsset(asset.chainId)
                }
                // todo check asset balance
                asset.toAssetItem(chainIconUrl)
            }
            val localExistsIds = arrayListOf<String>()
            val onlyRemoteItems = arrayListOf<AssetItem>()
            val needUpdatePrice = arrayListOf<PriceAndChange>()
            assetItemList.forEach {
                val exists = findAssetItemById(it.assetId)
                if (exists != null) {
                    needUpdatePrice.add(it.toPriceAndChange())
                    localExistsIds.add(exists.assetId)
                } else {
                    onlyRemoteItems.add(it)
                }
            }
            return if (needUpdatePrice.isNotEmpty()) {
                suspendUpdatePrices(needUpdatePrice)
                onlyRemoteItems + findAssetsByIds(localExistsIds)
            } else {
                assetItemList
            }
        }
        return emptyList()
    }

    private suspend fun fetchAsset(assetId: String) = withContext(Dispatchers.IO) {
        val r = try {
            asset(assetId)
        } catch (t: Throwable) {
            ErrorHandler.handleError(t)
            return@withContext null
        }
        if (r.isSuccess) {
            r.data?.let {
                insert(it)
                return@withContext it.iconUrl
            }
        } else {
            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
        }
        return@withContext null
    }

    private suspend fun queryAssets(query: String) = assetService.queryAssets(query)

    private suspend fun getIconUrl(id: String) = assetDao.getIconUrl(id)

    fun observeTopAssets() = hotAssetDao.topAssets()

    fun checkExists(id: String) = assetDao.checkExists(id)

    suspend fun findAddressById(addressId: String, assetId: String) =
        addressDao.findAddressById(addressId, assetId)

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
            },
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
            },
        )
        return result
    }

    suspend fun getSnapshots(
        assetId: String,
        offset: String?,
        limit: Int,
        opponent: String?,
        destination: String?,
        tag: String?
    ) =
        assetService.getSnapshots(assetId, offset, limit, opponent, destination, tag)

    suspend fun insertTrace(trace: Trace) = traceDao.insertSuspend(trace)

    suspend fun suspendFindTraceById(traceId: String): Trace? =
        traceDao.suspendFindTraceById(traceId)

    suspend fun getTrace(traceId: String) = assetService.getTrace(traceId)

    suspend fun findLatestTrace(
        opponentId: String?,
        destination: String?,
        tag: String?,
        amount: String,
        assetId: String
    ): Pair<Trace?, Boolean> {
        val trace =
            traceDao.suspendFindTrace(opponentId, destination, tag, amount, assetId) ?: return Pair(
                null,
                false
            )

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

    suspend fun ticker(tickerRequest: RouteTickerRequest): MixinResponse<RouteTickerResponse> =
        routeService.ticker(tickerRequest)

    suspend fun findSnapshotByTransactionHashList(
        assetId: String,
        hashList: List<String>
    ): List<String> =
        snapshotDao.findSnapshotIdsByTransactionHashList(assetId, hashList)

    suspend fun suspendUpdatePrices(priceAndChange: List<PriceAndChange>) =
        assetDao.suspendUpdatePrices(priceAndChange)

    suspend fun findTotalUSDBalance(): Int = assetDao.findTotalUSDBalance() ?: 0

    suspend fun findAllAssetIdSuspend() = assetDao.findAllAssetIdSuspend()

    suspend fun findAssetIdByAssetKey(assetKey: String): String? =
        assetDao.findAssetIdByAssetKey(assetKey)

    suspend fun refreshAsset(assetId: String): Token? =
        handleMixinResponse(
            invokeNetwork = { assetService.getAssetByIdSuspend(assetId) },
            switchContext = Dispatchers.IO,
            successBlock = {
                it.data?.let { a ->
                    tokeDao.upsertSuspend(a)
                    return@handleMixinResponse a
                }
            },
        )

    suspend fun token(): MixinResponse<RouteTokenResponse> = routeService.sumsubToken()

    fun callSumsubToken(): Call<MixinResponse<RouteTokenResponse>> = routeService.callSumsubToken()

    suspend fun profile(): MixinResponse<ProfileResponse> = routeService.profile()

    suspend fun payment(traceRequest: RoutePaymentRequest): MixinResponse<RoutePaymentResponse> =
        routeService.payment(traceRequest)

    suspend fun payment(paymentId: String): MixinResponse<RoutePaymentResponse> =
        routeService.payment(paymentId)

    suspend fun payments(): MixinResponse<List<RoutePaymentResponse>> = routeService.payments()

    suspend fun createSession(createSession: RouteSessionRequest): MixinResponse<RouteSessionResponse> =
        routeService.createSession(createSession)

    suspend fun token(tokenRequest: RouteTokenRequest) = routeService.token(tokenRequest)

    suspend fun createInstrument(createInstrument: RouteInstrumentRequest): MixinResponse<Card> =
        routeService.createInstrument(createInstrument)

    suspend fun getSession(sessionId: String): MixinResponse<RouteSessionResponse> =
        routeService.getSession(sessionId)

    fun cards(): Flow<SafeBox?> = safeBox.data

    suspend fun addCard(card: Card) {
        safeBox.updateData { box ->
            val list = box.cards.toMutableList()
            list.add(card)
            SafeBox(list)
        }
    }

    suspend fun removeCard(index: Int) {
        safeBox.updateData { box ->
            val list = box.cards.toMutableList()
            list.removeAt(index)
            SafeBox(list)
        }
    }

    suspend fun initSafeBox(cards: List<Card>) {
        safeBox.updateData { _ ->
            SafeBox(cards)
        }
    }

    suspend fun instruments(): MixinResponse<List<Card>> = routeService.instruments()

    suspend fun deleteInstruments(id: String): MixinResponse<Void> =
        routeService.deleteInstruments(id)
}
