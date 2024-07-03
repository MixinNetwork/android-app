package one.mixin.android.ui.oldwallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.Pin
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.request.RouteTokenRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.request.web3.StakeRequest
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.RouteOrderResponse
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.api.response.web3.QuoteResponse
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.service.AddressService
import one.mixin.android.api.service.AssetService
import one.mixin.android.api.service.RouteService
import one.mixin.android.db.AddressDao
import one.mixin.android.db.AssetDao
import one.mixin.android.db.ChainDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.db.TraceDao
import one.mixin.android.extension.within6Hours
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.ErrorHandler.Companion.NOT_FOUND
import one.mixin.android.vo.Address
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.PriceAndChange
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.Trace
import one.mixin.android.vo.sumsub.RouteTokenResponse
import one.mixin.android.vo.toAssetItem
import one.mixin.android.vo.toPriceAndChange
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepository
    @Inject
    constructor(
        private val assetService: AssetService,
        private val routeService: RouteService,
        private val assetDao: AssetDao,
        private val snapshotDao: SnapshotDao,
        private val addressDao: AddressDao,
        private val addressService: AddressService,
        private val traceDao: TraceDao,
        private val chainDao: ChainDao,
    ) {
        fun assets() = assetService.assets()

        suspend fun simpleAssetsWithBalance() = assetDao.simpleAssetsWithBalance()

        fun assetsWithBalance() = assetDao.assetsWithBalance()

        fun insert(asset: Asset) {
            assetDao.insert(asset)
        }

        fun insertList(asset: List<Asset>) {
            assetDao.insertList(asset)
        }

        suspend fun snapshotLocal(
            assetId: String,
            snapshotId: String,
        ) = snapshotDao.snapshotLocal(assetId, snapshotId)

        suspend fun asset(id: String) = assetService.getAssetByIdSuspend(id)

        suspend fun getAssetPrecisionById(id: String) = assetService.getAssetPrecisionById(id)

        suspend fun findOrSyncAsset(assetId: String): AssetItem? {
            var assetItem = assetDao.findAssetItemById(assetId)
            if (assetItem != null && assetItem.getDestination().isNotBlank()) return assetItem

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

        suspend fun syncAsset(assetId: String): AssetItem? {
            val asset: Asset =
                handleMixinResponse(
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

        fun insertSnapshot(snapshot: Snapshot) = snapshotDao.insert(snapshot)

        suspend fun transfer(transferRequest: TransferRequest) = assetService.transfer(transferRequest)

        suspend fun paySuspend(request: TransferRequest) = assetService.paySuspend(request)

        fun addresses(id: String) = addressDao.addresses(id)

        suspend fun withdrawal(withdrawalRequest: WithdrawalRequest) = assetService.withdrawals(withdrawalRequest)

        fun saveAddr(addr: Address) = addressDao.insert(addr)

        suspend fun syncAddr(addressRequest: AddressRequest) = addressService.addresses(addressRequest)

        suspend fun deleteAddr(
            id: String,
            pin: String,
        ) = addressService.delete(id, Pin(pin))

        suspend fun deleteLocalAddr(id: String) = addressDao.deleteById(id)

        fun assetItems() = assetDao.assetItems()

        fun assetItems(assetIds: List<String>) = assetDao.assetItems(assetIds)

        suspend fun fuzzySearchAssetIgnoreAmount(query: String) = assetDao.fuzzySearchAssetIgnoreAmount(query, query)

        fun assetItem(id: String) = assetDao.assetItem(id)

        suspend fun simpleAssetItem(id: String) = assetDao.simpleAssetItem(id)

        fun assetItemsWithBalance() = assetDao.assetItemsWithBalance()

        suspend fun queryAsset(query: String): List<AssetItem> {
            val response =
                try {
                    queryAssets(query)
                } catch (t: Throwable) {
                    ErrorHandler.handleError(t)
                    return emptyList()
                }
            if (response.isSuccess) {
                val assetList = response.data as List<Asset>
                if (assetList.isEmpty()) {
                    return emptyList()
                }
                val assetItemList = arrayListOf<AssetItem>()
                assetList.mapTo(assetItemList) { asset ->
                    var chainIconUrl = getIconUrl(asset.chainId)
                    if (chainIconUrl == null) {
                        chainIconUrl = fetchAsset(asset.chainId)
                    }
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

        private suspend fun fetchAsset(assetId: String) =
            withContext(Dispatchers.IO) {
                val r =
                    try {
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

        suspend fun findAddressById(
            addressId: String,
            assetId: String,
        ) = addressDao.findAddressById(addressId, assetId)

        suspend fun refreshAndGetAddress(
            addressId: String,
            assetId: String,
        ): Pair<Address?, Boolean> {
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

        suspend fun insertTrace(trace: Trace) = traceDao.insertSuspend(trace)

        suspend fun suspendFindTraceById(traceId: String): Trace? = traceDao.suspendFindTraceById(traceId)

        suspend fun getTrace(traceId: String) = assetService.getTrace(traceId)

        suspend fun findLatestTrace(
            opponentId: String?,
            destination: String?,
            tag: String?,
            amount: String,
            assetId: String,
        ): Pair<Trace?, Boolean> {
            val trace = traceDao.suspendFindTrace(opponentId, destination, tag, amount, assetId) ?: return Pair(null, false)

            val with6hours = trace.createdAt.within6Hours()
            if (!with6hours) {
                return Pair(null, false)
            }

            if (trace.snapshotId.isNullOrBlank()) {
                val response =
                    try {
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

        suspend fun ticker(
            assetId: String,
            offset: String?,
        ) = assetService.ticker(assetId, offset)

        suspend fun ticker(tickerRequest: RouteTickerRequest): MixinResponse<RouteTickerResponse> = routeService.ticker(tickerRequest)

        suspend fun suspendUpdatePrices(priceAndChange: List<PriceAndChange>) =
            assetDao.suspendUpdatePrices(priceAndChange)

        suspend fun findTotalUSDBalance(): Int = assetDao.findTotalUSDBalance() ?: 0

        suspend fun findAllAssetIdSuspend() = assetDao.findAllAssetIdSuspend()

        suspend fun findAssetIdByAssetKey(assetKey: String): String? =
            assetDao.findAssetIdByAssetKey(assetKey)

        suspend fun refreshAsset(assetId: String): Asset? =
            handleMixinResponse(
                invokeNetwork = { assetService.getAssetByIdSuspend(assetId) },
                switchContext = Dispatchers.IO,
                successBlock = {
                    it.data?.let { a ->
                        assetDao.upsertSuspend(a)
                        return@handleMixinResponse a
                    }
                },
            )

        suspend fun token(): MixinResponse<RouteTokenResponse> = routeService.sumsubToken()

        // suspend fun payment(traceRequest: RoutePaymentRequest): MixinResponse<RoutePaymentResponse> = routeService.payment(traceRequest)

        suspend fun order(paymentId: String): MixinResponse<RouteOrderResponse> = routeService.order(paymentId)

        suspend fun token(tokenRequest: RouteTokenRequest) = routeService.token(tokenRequest)

        fun observeAddress(addressId: String) = addressDao.observeById(addressId)

        suspend fun web3Tokens(source: String): MixinResponse<List<SwapToken>> = routeService.web3Tokens(source)

        suspend fun web3Quote(
            inputMint: String,
            outputMint: String,
            amount: String,
            slippage: String,
            source: String,
        ): MixinResponse<QuoteResponse> = routeService.web3Quote(inputMint, outputMint, amount, slippage, source)

        suspend fun web3Swap(
            swapRequest: SwapRequest,
        ): MixinResponse<SwapResponse> = routeService.web3Swap(swapRequest)

        suspend fun getWeb3Tx(txhash: String) = routeService.getWeb3Tx(txhash)

        suspend fun getSwapToken(address: String) = routeService.getSwapToken(address)

        suspend fun searchTokens(query: String) = routeService.searchTokens(query)

        suspend fun stakeSol(stakeRequest: StakeRequest) = routeService.stakeSol(stakeRequest)

        suspend fun getStakeAccounts(account: String) = routeService.getStakeAccounts(account)

        suspend fun getStakeAccountActivations(accounts: String) = routeService.getStakeAccountActivations(accounts)

        suspend fun getStakeValidators(votePubkeys: String?) = routeService.getStakeValidators(votePubkeys)
    }
