package one.mixin.android.repository

import android.os.CancellationSignal
import androidx.datastore.core.DataStore
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.liveData
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig.VERSION_NAME
import one.mixin.android.Constants
import one.mixin.android.Constants.SAFE_PUBLIC_KEY
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.DepositEntryRequest
import one.mixin.android.api.request.GhostKeyRequest
import one.mixin.android.api.request.OrderRequest
import one.mixin.android.api.request.Pin
import one.mixin.android.api.request.RampWebUrlRequest
import one.mixin.android.api.request.RouteInstrumentRequest
import one.mixin.android.api.request.RoutePriceRequest
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.request.RouteTokenRequest
import one.mixin.android.api.request.TransactionRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.request.web3.Web3RawTransactionRequest
import one.mixin.android.api.response.RampWebUrlResponse
import one.mixin.android.api.response.RouteOrderResponse
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.api.response.TransactionResponse
import one.mixin.android.api.response.WithdrawalResponse
import one.mixin.android.api.response.web3.ParsedTx
import one.mixin.android.api.service.AddressService
import one.mixin.android.api.service.AssetService
import one.mixin.android.api.service.MemberService
import one.mixin.android.api.service.RouteService
import one.mixin.android.api.service.TokenService
import one.mixin.android.api.service.UserService
import one.mixin.android.api.service.UtxoService
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.crypto.verifyCurve25519Signature
import one.mixin.android.db.AddressDao
import one.mixin.android.db.AlertDao
import one.mixin.android.db.ChainDao
import one.mixin.android.db.DepositDao
import one.mixin.android.db.HistoryPriceDao
import one.mixin.android.db.InscriptionCollectionDao
import one.mixin.android.db.InscriptionDao
import one.mixin.android.db.MarketCoinDao
import one.mixin.android.db.MarketDao
import one.mixin.android.db.MarketFavoredDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.OutputDao
import one.mixin.android.db.RawTransactionDao
import one.mixin.android.db.SafeSnapshotDao
import one.mixin.android.db.SwapOrderDao
import one.mixin.android.db.TokenDao
import one.mixin.android.db.TokensExtraDao
import one.mixin.android.db.TopAssetDao
import one.mixin.android.db.TraceDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.db.property.Web3PropertyHelper
import one.mixin.android.db.provider.DataProvider
import one.mixin.android.db.web3.Web3AddressDao
import one.mixin.android.db.web3.Web3RawTransactionDao
import one.mixin.android.db.web3.Web3TokenDao
import one.mixin.android.db.web3.Web3TransactionDao
import one.mixin.android.db.web3.Web3WalletDao
import one.mixin.android.db.web3.vo.AssetChange
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3RawTransaction
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.hexString
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.toast
import one.mixin.android.extension.within6Hours
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncInscriptionMessageJob
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.ui.home.web3.widget.MarketSort
import one.mixin.android.ui.wallet.FilterParams
import one.mixin.android.ui.wallet.Web3FilterParams
import one.mixin.android.ui.wallet.adapter.SnapshotsMediator
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertRequest
import one.mixin.android.ui.wallet.alert.vo.AlertUpdateRequest
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.ErrorHandler.Companion.NOT_FOUND
import one.mixin.android.vo.Address
import one.mixin.android.vo.AddressItem
import one.mixin.android.vo.Card
import one.mixin.android.vo.InscriptionCollection
import one.mixin.android.vo.InscriptionItem
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.PriceAndChange
import one.mixin.android.vo.SafeBox
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.Trace
import one.mixin.android.vo.UtxoItem
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketCoin
import one.mixin.android.vo.market.MarketFavored
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.route.RoutePaymentRequest
import one.mixin.android.vo.route.SwapOrderItem
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.Output
import one.mixin.android.vo.safe.RawTransaction
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.SafeCollection
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.SafeSnapshotType
import one.mixin.android.vo.safe.SafeWithdrawal
import one.mixin.android.vo.safe.Token
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.TokensExtra
import one.mixin.android.vo.safe.UnifiedAssetItem
import one.mixin.android.vo.safe.toAssetItem
import one.mixin.android.vo.safe.toPriceAndChange
import one.mixin.android.vo.sumsub.ProfileResponse
import one.mixin.android.vo.sumsub.RouteTokenResponse
import one.mixin.android.web3.js.JsSigner
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRepository
    @Inject
    constructor(
        private val appDatabase: MixinDatabase,
        private val tokenService: TokenService,
        private val assetService: AssetService,
        private val utxoService: UtxoService,
        private val userService: UserService,
        private val routeService: RouteService,
        private val tokenDao: TokenDao,
        private val tokensExtraDao: TokensExtraDao,
        private val safeSnapshotDao: SafeSnapshotDao,
        private val addressDao: AddressDao,
        private val addressService: AddressService,
        private val hotAssetDao: TopAssetDao,
        private val traceDao: TraceDao,
        private val chainDao: ChainDao,
        private val depositDao: DepositDao,
        private val rawTransactionDao: RawTransactionDao,
        private val outputDao: OutputDao,
        private val userDao: UserDao,
        private val inscriptionDao: InscriptionDao,
        private val inscriptionCollectionDao: InscriptionCollectionDao,
        private val historyPriceDao: HistoryPriceDao,
        private val marketDao: MarketDao,
        private val marketCoinDao: MarketCoinDao,
        private val marketFavoredDao: MarketFavoredDao,
        private val alertDao: AlertDao,
        private val swapOrderDao: SwapOrderDao,
        private val web3TokenDao: Web3TokenDao,
        private val web3TransactionDao: Web3TransactionDao,
        private val web3RawTransactionDao: Web3RawTransactionDao,
        private val web3WalletDao: Web3WalletDao,
        private val jobManager: MixinJobManager,
        private val safeBox: DataStore<SafeBox>,
        private val web3AddressDao: Web3AddressDao,
    ) {
        fun assets() = tokenService.assets()

        suspend fun simpleAssetsWithBalance() = tokenDao.simpleAssetsWithBalance()

        suspend fun tokenEntry(ids: Array<String>) = tokenDao.tokenEntry(ids)

        suspend fun tokenEntry() = tokenDao.tokenEntry()

        fun insert(asset: Token) {
            tokenDao.insert(asset)
        }

        fun insertList(asset: List<Token>) {
            tokenDao.insertList(asset)
        }

        suspend fun asset(id: String) = tokenService.getAssetByIdSuspend(id)

        suspend fun getAssetPrecisionById(id: String) = tokenService.getAssetPrecisionById(id)

        suspend fun findOrSyncAsset(
            assetId: String,
        ): TokenItem? {
            var assetItem = tokenDao.findAssetItemById(assetId)
            if (assetItem == null) {
                assetItem = syncAsset(assetId)
            }
            if (assetItem != null && assetItem.chainId != assetItem.assetId && simpleAsset(assetItem.chainId) == null) {
                val chain = syncAsset(assetItem.chainId)
                assetItem.chainIconUrl = chain?.chainIconUrl
                assetItem.chainSymbol = chain?.chainSymbol
                assetItem.chainName = chain?.chainName
            }
            return assetItem
        }

        suspend fun findOrSyncAssetByInscription(
            collectionHash: String, instantiationHash: String
        ): TokenItem? {
            var assetItem = tokenDao.findAssetItemByCollectionHash(collectionHash)
            val output = outputDao.findOutputByHash(instantiationHash) ?: return null
            if (assetItem == null) {
                assetItem = syncAssetByKernel(output.asset)

            }
            if (assetItem != null && assetItem.chainId != assetItem.assetId && simpleAsset(assetItem.chainId) == null) {
                val chain = syncAsset(assetItem.chainId)
                assetItem.chainIconUrl = chain?.chainIconUrl
                assetItem.chainSymbol = chain?.chainSymbol
                assetItem.chainName = chain?.chainName
            }
            return assetItem
        }

        suspend fun syncDepositEntry(chainId: String, assetId: String?): Pair<DepositEntry?, Int> {
            var code = 200
            val depositEntry =
                handleMixinResponse(
                    invokeNetwork = {
                        utxoService.createDeposit(
                            DepositEntryRequest(chainId, assetId),
                        )
                    },
                    failureBlock = {
                        code = it.errorCode
                        code == ErrorHandler.ADDRESS_GENERATING
                    },
                    successBlock = { resp ->
                        val pubs = SAFE_PUBLIC_KEY.map { it.hexStringToByteArray() }
                        resp.data?.filter {
                            val message =
                                if (it.tag.isNullOrBlank()) {
                                    it.destination
                                } else {
                                    "${it.destination}:${it.tag}"
                                }.toByteArray().sha3Sum256()
                            val signature = it.signature.hexStringToByteArray()
                            pubs.any { pub -> verifyCurve25519Signature(message, signature, pub) }
                        }?.let { list ->
                            depositDao.insertAll(chainId, list)
                            list.find { it.isPrimary }
                        }
                    },
                )
            return Pair(depositEntry, code)
        }

        suspend fun findDepositEntry(chainId: String) = depositDao.findDepositEntry(chainId)

        suspend fun findDepositEntryDestinations() = depositDao.findDepositEntryDestinations()

        suspend fun findAndSyncDepositEntry(chainId: String, assetId: String?): Triple<DepositEntry?, Boolean, Int> {
            val oldDeposit = depositDao.findDepositEntry(chainId)
            val (newDeposit, code) = syncDepositEntry(chainId, assetId)
            val result =
                if (code != 200) {
                    null // response error
                } else {
                    newDeposit ?: oldDeposit
                }
            return Triple(result, newDeposit != null && oldDeposit != null && (oldDeposit.destination != newDeposit.destination || oldDeposit.tag != newDeposit.tag), code)
        }

        suspend fun syncAsset(assetId: String): TokenItem? {
            val asset: Token =
                handleMixinResponse(
                    invokeNetwork = {
                        tokenService.getAssetByIdSuspend(assetId)
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
                        tokenService.getChainById(asset.chainId)
                    },
                    successBlock = { resp ->
                        resp.data?.let { c ->
                            chainDao.upsertSuspend(c)
                        }
                    },
                )
            }

            return tokenDao.findAssetItemById(assetId)
        }

        suspend fun syncAndFindTokens(assetIds: List<String>): List<TokenItem> {
            handleMixinResponse(
                invokeNetwork = { tokenService.fetchTokenSuspend(assetIds) },
                successBlock = { resp ->
                   resp.data?.let { list ->
                       tokenDao.insertListSuspend(list)
                       list
                   }
                }
            ) ?: return emptyList()
            return tokenDao.findTokenItems(assetIds)
        }

        suspend fun checkAndSyncTokens(assetIds: List<String>) {
            val list = tokenDao.findTokenItems(assetIds)
            if (list.size == assetIds.size) return
            handleMixinResponse(
                invokeNetwork = { tokenService.fetchTokenSuspend(assetIds) },
                successBlock = { resp ->
                    resp.data?.let { list ->
                        tokenDao.insertListSuspend(list)
                        list
                    }
                }
            )
        }

        suspend fun syncAssetByKernel(kernelAssetId: String): TokenItem? {
            val asset: Token =
                handleMixinResponse(
                    invokeNetwork = {
                        tokenService.getAssetByIdSuspend(kernelAssetId)
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
                        tokenService.getChainById(asset.chainId)
                    },
                    successBlock = { resp ->
                        resp.data?.let { c ->
                            chainDao.upsertSuspend(c)
                        }
                    },
                )
            }

            return tokenDao.findTokenItemByAsset(kernelAssetId)
        }

        private suspend fun simpleAsset(id: String) = tokenDao.simpleAsset(id)

        suspend fun insertPendingDeposit(snapshot: List<SafeSnapshot>) = safeSnapshotDao.insertPendingDeposit(snapshot)

        @ExperimentalPagingApi
        fun snapshots(
            assetId: String,
            type: String? = null,
            otherType: String? = null,
            orderByAmount: Boolean = false,
        ): LiveData<PagingData<SnapshotItem>> =
            Pager(
                config =
                    PagingConfig(
                        pageSize = Constants.PAGE_SIZE,
                        enablePlaceholders = true,
                    ),
                pagingSourceFactory = {
                    if (type == null) {
                        if (orderByAmount) {
                            safeSnapshotDao.snapshotsOrderByAmountPaging(assetId)
                        } else {
                            safeSnapshotDao.snapshotsPaging(assetId)
                        }
                    } else {
                        if (orderByAmount) {
                            safeSnapshotDao.snapshotsByTypeOrderByAmountPaging(assetId, type, otherType)
                        } else {
                            safeSnapshotDao.snapshotsByTypePaging(assetId, type, otherType)
                        }
                    }
                },
                remoteMediator =
                    SnapshotsMediator(
                        tokenService,
                        safeSnapshotDao,
                        tokenDao,
                        jobManager,
                        assetId,
                    ),
            ).liveData

        fun snapshotsLimit(id: String) = safeSnapshotDao.snapshotsLimit(id)

        suspend fun snapshotLocal(
            assetId: String,
            snapshotId: String,
        ) =
            safeSnapshotDao.snapshotLocal(assetId, snapshotId)

        fun findAddressByReceiver(receiver: String, tag: String) = addressDao.findAddressByReceiver(receiver, tag)

        fun insertSnapshot(snapshot: SafeSnapshot) = safeSnapshotDao.insert(snapshot)

        fun getXIN() = tokenDao.getXIN()

        suspend fun paySuspend(request: TransferRequest) = tokenService.paySuspend(request)

        suspend fun updateHidden(
            id: String,
            hidden: Boolean,
        ) {
            appDatabase.withTransaction {
                val tokensExtra = tokensExtraDao.findByAssetId(id)
                if (tokensExtra != null) {
                    tokensExtraDao.updateHiddenByAssetId(id, hidden)
                } else {
                    tokensExtraDao.insertSuspend(TokensExtra(id, assetIdToAsset(id), hidden, "0", nowInUtc()))
                }
            }
        }

        fun hiddenAssetItems() = tokenDao.hiddenAssetItems()

        fun addresses(id: String) = addressDao.addresses(id)

        fun addressesFlow(id: String) = addressDao.addressesFlow(id)

        fun observeAddress(addressId: String) = addressDao.observeById(addressId)

        fun saveAddr(addr: Address) = addressDao.insert(addr)

        suspend fun syncAddr(addressRequest: AddressRequest) = addressService.addresses(addressRequest)

        suspend fun deleteAddr(
            id: String,
            pin: String,
        ) = addressService.delete(id, Pin(pin))

        suspend fun deleteLocalAddr(id: String) = addressDao.deleteById(id)

        fun assetItemsNotHidden() = tokenDao.assetItemsNotHidden()

        fun assetItems() = tokenDao.assetItems()

        fun coinItems() = marketDao.coinItems()

        suspend fun allAssetItems() = tokenDao.allAssetItems()

        fun assetItems(assetIds: List<String>) = tokenDao.assetItems(assetIds)

        suspend fun findTokenItems(ids: List<String>): List<TokenItem> = tokenDao.findTokenItems(ids)

        suspend fun findAssetItemsWithBalance(): List<TokenItem> = tokenDao.findAssetItemsWithBalance()

        suspend fun findUnifiedAssetItem(): List<UnifiedAssetItem> = tokenDao.findUnifiedAssetItem()

        suspend fun findWeb3AssetItemsWithBalance(walletId: String): List<Web3TokenItem> = web3TokenDao.findAssetItemsWithBalance(walletId)

        suspend fun web3TokenItems(chainIds: List<String>): List<TokenItem> = tokenDao.web3TokenItems(chainIds)

        fun web3TokenItems(walletId: String): LiveData<List<Web3TokenItem>> = web3TokenDao.web3TokenItems(walletId)

        suspend fun fuzzySearchToken(
            query: String,
            cancellationSignal: CancellationSignal,
        ) =
            DataProvider.fuzzySearchToken(query, query, appDatabase, cancellationSignal)

        suspend fun fuzzySearchAssetIgnoreAmount(query: String) =
            tokenDao.fuzzySearchAssetIgnoreAmount(query, query)

        fun assetItem(id: String) = tokenDao.assetItem(id)

        suspend fun simpleAssetItem(id: String) = tokenDao.simpleAssetItem(id)

        fun assetItemsWithBalance() = tokenDao.assetItemsWithBalance()

        fun allSnapshots(filterParams: FilterParams): DataSource.Factory<Int, SnapshotItem> {
            return safeSnapshotDao.getSnapshots(filterParams.buildQuery()).map {
                if (!it.withdrawal?.receiver.isNullOrBlank()) {
                    val receiver = it.withdrawal!!.receiver
                    val index: Int = receiver.indexOf(":")
                    if (index == -1) {
                        it.label = addressDao.findAddressByReceiver(receiver, "")
                    } else {
                        val destination: String = receiver.substring(0, index)
                        val tag: String = receiver.substring(index + 1)
                        it.label = addressDao.findAddressByReceiver(destination, tag)
                    }
                }
                it
            }
        }


        fun allWeb3Transaction(filterParams: Web3FilterParams): DataSource.Factory<Int, Web3TransactionItem> {
            return web3TransactionDao.allTransactions(filterParams.buildQuery())
        }

        suspend fun deleteAllWeb3Transactions() {
            val addresses = web3AddressDao.getAddress()
            web3TransactionDao.deleteAllTransactions()
            addresses.forEach { address ->
                Web3PropertyHelper.deleteKeyValue(address.destination)
            }
        }

        suspend fun deleteWallets() {
            web3WalletDao.deleteAllWallets()
        }

        suspend fun getRawTransactionByHashAndChain(hash: String, chainId: String) = web3RawTransactionDao.getRawTransactionByHashAndChain(JsSigner.currentWalletId, hash, chainId)

        fun snapshotsByUserId(opponentId: String) = safeSnapshotDao.snapshotsByUserId(opponentId)

        suspend fun allPendingDeposit() = tokenService.allPendingDeposits()

        fun getPendingDisplays() = safeSnapshotDao.getPendingDisplays()

        suspend fun pendingDeposits(
            asset: String,
            destination: String,
            tag: String? = null,
        ) =
            tokenService.pendingDeposits(asset, destination, tag)

        suspend fun clearAllPendingDeposits() = safeSnapshotDao.clearAllPendingDeposits()

        suspend fun clearPendingDepositsByAssetId(assetId: String) =
            safeSnapshotDao.clearPendingDepositsByAssetId(assetId)

        suspend fun queryAsset(walletId: String?, query: String, web3: Boolean = false): List<TokenItem> {
            val localLike = if (web3) {
                web3TokenDao.fuzzySearchAsset(walletId ?: "", query, query).map { t -> t.toTokenItem() }
            } else emptyList()

            val response =
                try {
                    queryAssets(query)
                } catch (t: Throwable) {
                    ErrorHandler.handleError(t)
                    return  localLike
                }
            if (response.isSuccess) {
                val assetList = response.data as List<Token>
                if (assetList.isEmpty()) {
                    return  localLike
                }
                val tokenItemList = arrayListOf<TokenItem>()
                assetList.mapTo(tokenItemList) { asset ->
                    var chainIconUrl = getIconUrl(asset.chainId)
                    if (chainIconUrl == null) {
                        chainIconUrl = fetchAsset(asset.chainId)
                    }
                    asset.toAssetItem(chainIconUrl)
                }
                val localExistsIds = arrayListOf<String>()
                val onlyRemoteItems = arrayListOf<TokenItem>()
                val needUpdatePrice = arrayListOf<PriceAndChange>()
                tokenItemList.forEach {
                    val exists = if (web3) null else findAssetItemById(it.assetId) // local
                    if (exists != null) {
                        needUpdatePrice.add(it.toPriceAndChange())
                        localExistsIds.add(exists.assetId)
                    } else {
                        onlyRemoteItems.add(it)
                    }
                }

                val result = if (needUpdatePrice.isNotEmpty()) {
                    suspendUpdatePrices(needUpdatePrice)
                    onlyRemoteItems + findAssetsByIds(localExistsIds)
                } else {
                    tokenItemList
                }
                return if (web3) {
                    (result + localLike
                        .filter { t -> result.any { r -> r.assetId == t.assetId }.not() })
                } else {
                    result
                }
            }
            return localLike
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

        private suspend fun queryAssets(query: String) = tokenService.queryAssets(query)

        private suspend fun getIconUrl(id: String) = tokenDao.getIconUrl(id)

        fun observeTopAssets() = hotAssetDao.topAssets()

        fun checkExists(id: String) = tokenDao.checkExists(id)

        suspend fun findAssetItemById(assetId: String) = tokenDao.findAssetItemById(assetId)

        suspend fun findAssetItemByCollectionHash(collectionHash: String) = tokenDao.findAssetItemByCollectionHash(collectionHash)

        suspend fun findAssetsByIds(assetIds: List<String>) = tokenDao.suspendFindAssetsByIds(assetIds)

        suspend fun findSnapshotById(snapshotId: String) = safeSnapshotDao.findSnapshotById(snapshotId)

        suspend fun findSnapshotByTraceId(traceId: String) = safeSnapshotDao.findSnapshotByTraceId(traceId)

        suspend fun refreshAndGetSnapshot(snapshotId: String): SnapshotItem? {
            var result: SnapshotItem? = null
            handleMixinResponse(
                invokeNetwork = {
                    tokenService.getSnapshotById(snapshotId)
                },
                successBlock = { response ->
                    response.data?.let {
                        safeSnapshotDao.insert(it)
                        result = safeSnapshotDao.findSnapshotById(snapshotId)
                    }
                },
                defaultErrorHandle = {}
            )
            return result
        }

        suspend fun insertTrace(trace: Trace) = traceDao.insertSuspend(trace)

        suspend fun suspendFindTraceById(traceId: String): Trace? =
            traceDao.suspendFindTraceById(traceId)

        suspend fun getTransactionsById(traceId: String) = utxoService.getTransactionsById(traceId)

        suspend fun getListTransactionsById(traceId: String): MixinResponse<List<TransactionResponse>> {
            val response = utxoService.getTransactionsById(traceId)
            return if (response.isSuccess) {
                MixinResponse(Response.success(listOf(response.data!!)))
            } else {
                MixinResponse(response.error!!)
            }
        }

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
                            utxoService.getTransactionsById(trace.traceId)
                        }
                    } catch (t: Throwable) {
                        ErrorHandler.handleError(t)
                        return Pair(null, true)
                    }
                return if (response.isSuccess) {
                    val data = response.data!!
                    trace.snapshotId = data.getSnapshotId
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

        suspend fun transactionsFetch(traceIds: List<String>) = utxoService.transactionsFetch(traceIds)

        suspend fun deletePreviousTraces() = traceDao.deletePreviousTraces()

        suspend fun suspendDeleteTraceById(traceId: String) = traceDao.suspendDeleteById(traceId)

        suspend fun ticker(
            assetId: String,
            offset: String?,
        ) = tokenService.ticker(assetId, offset)

        suspend fun ticker(tickerRequest: RouteTickerRequest): MixinResponse<RouteTickerResponse> =
            routeService.ticker(tickerRequest)

        suspend fun suspendUpdatePrices(priceAndChange: List<PriceAndChange>) =
            tokenDao.suspendUpdatePrices(priceAndChange)

        suspend fun findTotalUSDBalance(): Int = tokenDao.findTotalUSDBalance() ?: 0

        suspend fun findAllAssetIdSuspend() = tokenDao.findAllAssetIdSuspend()

        suspend fun findAssetIdByAssetKey(assetKey: String): String? =
            tokenDao.findAssetIdByAssetKey(assetKey)

        suspend fun refreshAsset(assetId: String): Token? =
            handleMixinResponse(
                invokeNetwork = { tokenService.getAssetByIdSuspend(assetId) },
                switchContext = Dispatchers.IO,
                successBlock = {
                    it.data?.let { a ->
                        tokenDao.upsertSuspend(a)
                        return@handleMixinResponse a
                    }
                },
            )

        suspend fun token(): MixinResponse<RouteTokenResponse> = routeService.sumsubToken()

        fun callSumsubToken(): Call<MixinResponse<RouteTokenResponse>> = routeService.callSumsubToken()

        suspend fun profile(): MixinResponse<ProfileResponse> = routeService.profile(VERSION_NAME)

        suspend fun payment(
            id: String,
            paymentRequestst: RoutePaymentRequest,
        ): MixinResponse<RouteOrderResponse> = routeService.order(id, paymentRequestst)

        suspend fun orders(): MixinResponse<List<RouteOrderResponse>> = routeService.payments()

        fun swapOrders(): Flow<List<SwapOrderItem>> = swapOrderDao.orders()

        suspend fun createOrder(createSession: OrderRequest): MixinResponse<RouteOrderResponse> =
            routeService.createOrder(createSession)

        suspend fun token(tokenRequest: RouteTokenRequest) = routeService.token(tokenRequest)

        suspend fun createInstrument(createInstrument: RouteInstrumentRequest): MixinResponse<Card> =
            routeService.createInstrument(createInstrument)

        suspend fun getOrder(orderId: String): MixinResponse<RouteOrderResponse> =
            routeService.getOrder(orderId)

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

        suspend fun deleteInstruments(id: String): MixinResponse<Void> = routeService.deleteInstruments(id)

        suspend fun transactionRequest(transactionRequests: List<TransactionRequest>) = utxoService.transactionRequest(transactionRequests)

        suspend fun transactions(transactionRequests: List<TransactionRequest>) = utxoService.transactions(transactionRequests)

        suspend fun ghostKey(ghostKeyRequest: List<GhostKeyRequest>) = utxoService.ghostKey(ghostKeyRequest)

        suspend fun findOutputs(
            limit: Int,
            asset: String,
            inscriptionHash: String? = null,
        ) = if (inscriptionHash != null) {
            outputDao.findUnspentInscriptionByAssetHash(limit, asset, inscriptionHash)
        } else {
            outputDao.findUnspentOutputsByAsset(limit, asset)
        }

        suspend fun findUnspentOutputByHash(inscriptionHash: String) = outputDao.findUnspentOutputByHash(inscriptionHash)

        suspend fun findOutputByHash(inscriptionHash: String) = outputDao.findOutputByHash(inscriptionHash)

        fun findInscriptionByHash(inscriptionHash: String) = inscriptionDao.findInscriptionByHash(inscriptionHash)

        fun findInscriptionCollectionByHash(inscriptionHash: String) = inscriptionDao.findInscriptionCollectionByHash(inscriptionHash)

        suspend fun findTokenItemByAsset(kernelAssetId: String) = tokenDao.findTokenItemByAsset(kernelAssetId)

        fun insertOutput(output: Output) = outputDao.insert(output)

        fun insertDeposit(data: List<DepositEntry>) {
            depositDao.insertList(data)
        }

        fun insetRawTransaction(rawTransaction: RawTransaction) {
            rawTransactionDao.insert(rawTransaction)
        }

        fun updateRawTransaction(
            requestId: String,
            state: String,
        ) {
            rawTransactionDao.updateRawTransaction(requestId, state)
        }

        fun updateUtxoToSigned(ids: List<String>) {
            val changed = outputDao.updateUtxoToSigned(ids)
            if (changed != ids.size) {
                Timber.e("Update failed, ${ids.joinToString(", ")}")
                throw RuntimeException("Update failed, please try again")
            }
            val unSignedOutputs = outputDao.getUnsignedOutputs(ids)
            if (unSignedOutputs.isNotEmpty()) {
                Timber.e("Update failed, ${unSignedOutputs.joinToString(", ")}")
                throw RuntimeException("Update failed, please try again")
            }
        }

        suspend fun findOldAssets() = assetService.fetchAllAssetSuspend()

        fun insertSnapshotMessage(
            data: TransactionResponse,
            conversationId: String,
            inscriptionHash: String?,
        ) {
            val snapshotId = data.getSnapshotId
            if (conversationId != "" && data.userId.isUUID()) {
                val category =
                    if (inscriptionHash != null) {
                        MessageCategory.SYSTEM_SAFE_INSCRIPTION.name
                    } else {
                        MessageCategory.SYSTEM_SAFE_SNAPSHOT.name
                    }
                val message = createMessage(UUID.randomUUID().toString(), conversationId, data.userId, category, inscriptionHash ?: "", data.createdAt, MessageStatus.DELIVERED.name, SafeSnapshotType.snapshot.name, null, snapshotId)
                appDatabase.insertMessage(message)
                if (inscriptionHash != null) {
                    jobManager.addJobInBackground(SyncInscriptionMessageJob(conversationId, message.messageId, inscriptionHash, snapshotId))
                }
                MessageFlow.insert(message.conversationId, message.messageId)
            }
        }

        fun insertSafeSnapshot(
            snapshotId: String,
            userId: String,
            opponentId: String,
            transactionHash: String,
            requestId: String,
            assetId: String,
            amount: String,
            memo: String?,
            type: SafeSnapshotType,
            withdrawal: SafeWithdrawal? = null,
            reference: String? = null,
        ) {
            val snapshot = SafeSnapshot(snapshotId, type.name, assetId, "-$amount", userId, opponentId, memo?.toByteArray()?.hexString() ?: "", transactionHash, nowInUtc(), requestId, null, null, null, null, withdrawal, reference)
            safeSnapshotDao.insert(snapshot)
        }

        fun findUser(userId: String) = userDao.findUser(userId)

        fun findRawTransaction(traceId: String) = rawTransactionDao.findRawTransaction(traceId)

        suspend fun getFees(
            id: String,
            destination: String,
        ): MixinResponse<List<WithdrawalResponse>> {
            val r = tokenService.getFees(id, destination)
            if (r.isSuccess) {
                r.data?.forEach { fee ->
                    val assetId = fee.assetId
                    if (assetId.isNullOrEmpty().not()) {
                        withContext(Dispatchers.IO) {
                            findOrSyncAsset(assetId)
                        }
                    }
                    // do nothing
                }
            }
            return r
        }

        suspend fun getMultisigs(requestId: String) = utxoService.getMultisigs(requestId)

        suspend fun signTransactionMultisigs(
            requestId: String,
            transactionRequest: TransactionRequest,
        ) = utxoService.signTransactionMultisigs(requestId, transactionRequest)

        suspend fun unlockTransactionMultisigs(requestId: String) = utxoService.unlockTransactionMultisigs(requestId)

        fun utxoItem(asset: String): LiveData<PagingData<UtxoItem>> {
            return Pager(
                config =
                    PagingConfig(
                        pageSize = Constants.PAGE_SIZE,
                        enablePlaceholders = true,
                    ),
                pagingSourceFactory = {
                    outputDao.utxoItem(asset)
                },
            ).liveData
        }

        suspend fun removeUtxo(outputId: String) = outputDao.removeUtxo(outputId)

        fun firstUnspentTransaction() = rawTransactionDao.findUnspentTransaction()

        suspend fun findLastWithdrawalSnapshotByReceiver(formatDestination: String) = safeSnapshotDao.findLastWithdrawalSnapshotByReceiver(formatDestination)

        suspend fun findLatestOutputSequenceByAsset(asset: String) = outputDao.findLatestOutputSequenceByAsset(asset)

        fun insertOutputs(outputs: List<Output>) = outputDao.insertList(outputs)

        suspend fun deleteByKernelAssetIdAndOffset(
            asset: String,
            offset: Long,
        ) = outputDao.deleteByKernelAssetIdAndOffset(asset, offset)

        suspend fun getOutputs(
            members: String,
            threshold: Int,
            offset: Long? = null,
            limit: Int = 500,
            state: String? = null,
            asset: String? = null,
        ) = utxoService.getOutputs(
            members,
            threshold,
            offset,
            limit,
            state,
            asset,
        )

        suspend fun findTokensExtra(asset: String) = tokensExtraDao.findByAssetId(asset)

        suspend fun updatePrice(
            orderId: String,
            price: String,
        ) = routeService.updateOrderPrice(orderId, RoutePriceRequest(price))

        fun collectibles(sortOrder: SortOrder): LiveData<List<SafeCollectible>> = outputDao.collectibles(sortOrder.name)

        fun inscriptionItemsFlowByCollectionHash(collectionHash: String): Flow<List<InscriptionItem>> = inscriptionCollectionDao.inscriptionItemsFlowByCollectionHash(collectionHash)

        fun collectiblesByHash(collectionHash: String): LiveData<List<SafeCollectible>> = outputDao.collectiblesByHash(collectionHash)

        fun collections(sortOrder: SortOrder): LiveData<List<SafeCollection>> = outputDao.collections(sortOrder.name)

        fun collectionByHash(hash: String): LiveData<SafeCollection?> = outputDao.collectionByHash(hash)

        fun collectionFlowByHash(hash: String): Flow<InscriptionCollection?> = inscriptionCollectionDao.collectionFlowByHash(hash)

        fun inscriptionByHash(hash: String) = inscriptionDao.inscriptionByHash(hash)

        suspend fun fuzzyInscription(
            escapedQuery: String,
            cancellationSignal: CancellationSignal,
        ): List<SafeCollectible> {
            return DataProvider.fuzzyInscription(escapedQuery, appDatabase, cancellationSignal)
        }

        fun inscriptionStateByHash(hash: String) = outputDao.inscriptionStateByHash(hash)

        suspend fun getInscriptionItem(hash: String): InscriptionItem? {
            val response = tokenService.getInscriptionItem(hash)
            if (response.isSuccess) {
                inscriptionDao.insert(response.data!!)
                return response.data!!
            } else {
                return null
            }
        }

        suspend fun getInscriptionCollection(hash: String): InscriptionCollection? {
            val response = tokenService.getInscriptionCollection(hash)
            if (response.isSuccess) {
                inscriptionCollectionDao.insert(response.data!!)
                return response.data!!
            } else {
                return null
            }
        }

        suspend fun simulateWeb3Tx(parseTxRequest: Web3RawTransactionRequest): MixinResponse<ParsedTx> = routeService.simulateWeb3Tx(parseTxRequest)

        suspend fun postRawTx(rawTxRequest: Web3RawTransactionRequest, assetId: String? = null): MixinResponse<Web3RawTransaction> {
            val r = routeService.postWeb3Tx(rawTxRequest)
            if (r.isSuccess) {
                val raw = r.data!!
                web3RawTransactionDao.insertSuspend(raw)
                web3TransactionDao.deletePending(raw.hash, raw.chainId)

                val senders = mutableListOf<AssetChange>()
                val receivers = mutableListOf<AssetChange>()
                val approvals = mutableListOf<AssetChange>()

                raw.simulateTx?.balanceChanges?.forEach { bc ->
                    val amt = bc.amount.toBigDecimalOrNull()
                    if (amt != null) {
                        receivers.add(
                            AssetChange(
                                assetId = bc.assetId,
                                amount = amt.abs().toPlainString(),
                                from = bc.from,
                                to = bc.to
                            )
                        )
                        senders.add(
                            AssetChange(
                                assetId = bc.assetId,
                                amount = amt.toPlainString(),
                                from = bc.from,
                                to = bc.to,
                            )
                        )
                    }
                }

                raw.simulateTx?.approves?.forEach { approve ->
                    approvals.add(
                        AssetChange(
                            assetId = assetId ?: "",
                            amount = approve.amount,
                            from = raw.account,
                            to = approve.spender
                        )
                    )
                }

                var sendAssetId: String? = null
                var receiveAssetId: String? = null

                val txType = when {
                    raw.simulateTx?.approves?.isNotEmpty() == true -> TransactionType.APPROVAL.value
                    (raw.simulateTx?.balanceChanges?.size ?: 0) > 1 -> TransactionType.SWAP.value
                    raw.simulateTx?.balanceChanges?.size == 1 -> TransactionType.TRANSFER_OUT.value
                    else -> TransactionType.UNKNOWN.value
                }

                when (txType) {
                    TransactionType.SWAP.value -> {
                        raw.simulateTx?.balanceChanges?.forEach { bc ->
                            val amt = bc.amount.toBigDecimalOrNull()
                            if (amt != null) {
                                if (amt < java.math.BigDecimal.ZERO) {
                                    sendAssetId = bc.assetId
                                } else if (amt > java.math.BigDecimal.ZERO) {
                                    receiveAssetId = bc.assetId
                                }
                            }
                        }
                    }
                    TransactionType.TRANSFER_OUT.value -> {
                        raw.simulateTx?.balanceChanges?.firstOrNull {
                            it.amount.toBigDecimalOrNull()?.let { amt -> amt < java.math.BigDecimal.ZERO } == true
                        }?.let {
                            sendAssetId = it.assetId
                            receiveAssetId = it.assetId
                        }
                    }
                    else -> {
                        sendAssetId = assetId
                        receiveAssetId = assetId
                    }
                }

                web3TransactionDao.insert(
                    Web3Transaction(
                        transactionHash = raw.hash,
                        chainId = raw.chainId,
                        address = raw.account,
                        transactionType = txType,
                        status = TransactionStatus.PENDING.value,
                        blockNumber = 0,
                        fee = "",
                        senders = senders,
                        receivers = receivers,
                        approvals = approvals.ifEmpty { null },
                        sendAssetId = sendAssetId,
                        receiveAssetId = receiveAssetId,
                        transactionAt = raw.updatedAt,
                        createdAt = raw.createdAt,
                        updatedAt = raw.updatedAt,
                        level = Constants.AssetLevel.GOOD,
                    )
                )
            }
            return r
        }


        suspend fun refreshInscription(inscriptionHash: String): String? {
            val inscriptionItem = syncInscription(inscriptionHash) ?: return null
            val inscriptionCollection = syncInscriptionCollection(inscriptionItem.collectionHash) ?: return null
            val assetId = inscriptionCollection.kernelAssetId ?: return null
            syncAsset(assetId)
            return inscriptionItem.inscriptionHash
        }

        private suspend fun syncInscriptionCollection(collectionHash: String): InscriptionCollection? {
            val inscriptionCollection = inscriptionCollectionDao.findInscriptionCollectionByHash(collectionHash)
            if (inscriptionCollection == null) {
                val collectionResponse = tokenService.getInscriptionCollection(collectionHash)
                if (collectionResponse.isSuccess) {
                    val data = collectionResponse.data ?: return null
                    inscriptionCollectionDao.insert(data)
                    return data
                } else {
                    Timber.e(collectionResponse.errorDescription)
                    return null
                }
            } else {
                return inscriptionCollection
            }
        }

        private suspend fun syncInscription(inscriptionHash: String): InscriptionItem? {
            val inscriptionItem = inscriptionDao.findInscriptionByHash(inscriptionHash)
            if (inscriptionItem == null) {
                val inscriptionResponse = tokenService.getInscriptionItem(inscriptionHash)
                if (inscriptionResponse.isSuccess) {
                    val data = inscriptionResponse.data ?: return null
                    inscriptionDao.insert(data)
                    return data
                } else {
                    Timber.e(inscriptionResponse.errorDescription)
                    return null
                }
            } else {
                return inscriptionItem
            }
        }

    fun allAddresses(): LiveData<List<AddressItem>> = addressDao.allAddresses()

    suspend fun priceHistory(
        assetId: String,
        type: String,
    ) = routeService.priceHistory(assetId, type)

    fun marketById(
        assetId: String,
    ) = marketDao.marketById(assetId)

    fun marketByCoinId(
        coinId: String,
    ) = marketDao.marketByCoinId(coinId)

    fun historyPriceById(assetId: String) = historyPriceDao.historyPriceById(assetId)

    fun getWeb3Markets(limit: Int, sort: MarketSort): PagingSource<Int, MarketItem> = marketDao.getWeb3Markets(limit, sort.value)

    fun getFavoredWeb3Markets(sort: MarketSort): PagingSource<Int, MarketItem> = marketDao.getFavoredWeb3Markets(sort.value)

    suspend fun findTokensByCoinId(coinId: String) = marketCoinDao.findTokensByCoinId(coinId)

    suspend fun findTokenIdsByCoinId(coinId: String) = marketCoinDao.findTokenIdsByCoinId(coinId)

    suspend fun findMarketItemByAssetId(assetId: String) = marketDao.findMarketItemByAssetId(assetId)

    suspend fun findMarketItemByCoinId(coinId: String) = marketDao.findMarketItemByCoinId(coinId)

    suspend fun checkMarketById(id: String, force: Boolean = false): MarketItem? {
        val marketItem = if (force) {
            null
        } else if (id.isUUID()) {
            findMarketItemByAssetId(id)
        } else {
            findMarketItemByCoinId(id)
        }
        if (marketItem != null) return marketItem
        val response = routeService.market(id)
        if (response.isSuccess && response.data != null) {
            val data = response.data ?: return null
            val local = marketDao.findMarketById(data.coinId)
            val market = if (local != null) {
                data.copy(marketCapRank = local.marketCapRank)
            } else {
                data
            }
            marketDao.insert(market)
            marketCoinDao.insertIgnoreList(market.assetIds?.map { assetId ->
                MarketCoin(
                    coinId = market.coinId,
                    assetId = assetId,
                    createdAt = nowInUtc()
                )
            } ?: emptyList())
            return MarketItem.fromMarket(market)
        } else {
            return null
        }
    }

    suspend fun updateMarketFavored(symbol: String, coinId: String, isFavored: Boolean?) {
        val now = nowInUtc()
        if (isFavored == true) {
            requestRouteAPI(
                invokeNetwork = { routeService.unfavorite(coinId) },
                successBlock = { _ ->
                    marketFavoredDao.insert(
                        MarketFavored(
                            coinId = coinId,
                            isFavored = false,
                            now
                        )
                    )
                },
                requestSession = {
                    userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
                }
            )
        } else {
            requestRouteAPI(
                invokeNetwork = { routeService.favorite(coinId) },
                successBlock = { _ ->
                    marketFavoredDao.insert(
                        MarketFavored(
                            coinId = coinId,
                            isFavored = true,
                            now
                        )
                    )
                    withContext(Dispatchers.Main) {
                        toast(MixinApplication.appContext.getString(R.string.watchlist_add_desc, symbol))
                    }
                },
                requestSession = {
                    userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
                }
            )
        }
    }

    suspend fun addAlert(alert: AlertRequest): MixinResponse<Alert>? {
        return requestRouteAPI(
            invokeNetwork = { routeService.addAlert(alert) },
            successBlock = { response ->
                if (response.isSuccess) {
                    withContext(Dispatchers.IO) {
                        alertDao.insert(response.data!!)
                    }
                }
                response
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
            }
        )
    }

    fun alertGroups() = alertDao.alertGroups()

    fun alertGroups(coinIds: List<String>) = alertDao.alertGroups(coinIds)

    fun alertGroup(coinId: String) = alertDao.alertGroup(coinId)

    fun alertsByCoinId(coinId:String) = alertDao.alertsByCoinId(coinId)

    fun insertAlert(alert: Alert) = alertDao.insert(alert)

    suspend fun simpleCoinItem(coinId:String) = marketDao.simpleCoinItem(coinId)

    suspend fun simpleCoinItemByAssetId(assetId:String) = marketDao.simpleCoinItemByAssetId(assetId)

    fun anyAlertByCoinId(coinId: String) = alertDao.anyAlertByCoinId(coinId)

    fun anyAlertByAssetId(coinId: String) = alertDao.anyAlertByAssetId(coinId)

    suspend fun refreshMarket(
        coinId: String, endBlock: () -> Unit, failureBlock: (suspend (MixinResponse<Market>) -> Boolean),
        exceptionBlock: (suspend (t: Throwable) -> Boolean)
    ): MixinResponse<Market>? {
        return requestRouteAPI(
            invokeNetwork = { routeService.market(coinId) },
            exceptionBlock = exceptionBlock,
            failureBlock = failureBlock,
            endBlock = endBlock,
            successBlock = { response ->
                withContext(Dispatchers.IO){
                    val market = response.data!!
                    marketDao.insert(market)
                    marketCoinDao.insertList(market.assetIds?.map { assetId ->
                        MarketCoin(
                            coinId = market.coinId,
                            assetId = assetId,
                            createdAt = nowInUtc()
                        )
                    } ?: emptyList())
                }
                response
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
            }
        )
    }

    suspend fun updateAlert(alertId: String, request: AlertUpdateRequest): MixinResponse<Alert>? {
        return requestRouteAPI(
            invokeNetwork = { routeService.updateAlert(alertId, request) },
            successBlock = { response ->
                response
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
            }
        )
    }

    fun deleteAlertById(alertId: String) = alertDao.deleteAlertById(alertId)

    fun getTotalAlertCount(): Int {
        return alertDao.getTotalAlertCount()
    }

    fun getAlertCountByCoinId(coinId: String): Int {
        return alertDao.getAlertCountByCoinId(coinId)
    }

    suspend fun fuzzyMarkets(
        query: String,
        cancellationSignal: CancellationSignal,
    ): List<Market> =
        DataProvider.fuzzyMarkets(query, appDatabase, cancellationSignal)

    suspend fun searchMarket(query: String) = withContext(Dispatchers.IO){
        val response = routeService.searchMarket(query)
        response.data?.let { list->
            marketDao.upsertList(list)
            val now = nowInUtc()
            val ids = list.flatMap { market ->
                market.assetIds?.map { assetId ->
                    MarketCoin(
                        coinId = market.coinId,
                        assetId = assetId,
                        createdAt = now
                    )
                } ?: emptyList()
            }
            marketCoinDao.insertIgnoreList(ids)
        }
    }

    suspend fun findChangeUsdByAssetId(assetId: String) = tokenDao.findChangeUsdByAssetId(assetId)

    fun getOrderById(orderId: String): Flow<SwapOrderItem?> = swapOrderDao.getOrderById(orderId)

    fun tokenExtraFlow(asseId: String) = tokensExtraDao.tokenExtraFlow(asseId)

    fun web3TokenExtraFlow(walletId: String, asseId: String) = web3TokenDao.tokenExtraFlow(walletId, asseId)

    fun web3TokensFlow(walletId: String) = web3TokenDao.web3TokensFlow(walletId)

    suspend fun findWeb3TokenItems(walletId: String): List<Web3TokenItem> = web3TokenDao.findWeb3TokenItems(walletId)

    fun assetFlow() = tokenDao.assetFlow()

    suspend fun getSwapToken(address: String) = routeService.getSwapToken(address)

    suspend fun transaction(hash: String, chainId: String) = routeService.transaction(hash,chainId)

    suspend fun getPendingRawTransactions(walletId: String) = web3RawTransactionDao.getPendingRawTransactions(
        walletId)

    suspend fun getPendingTransactions(walletId: String) = web3TransactionDao.getPendingTransactions(walletId)

    suspend fun getPendingRawTransactions(walletId: String, chainId: String) = web3RawTransactionDao.getPendingRawTransactions(
        walletId, chainId)

    suspend fun deletePending(hash: String, chainId: String) = web3TransactionDao.deletePending(hash, chainId)

    suspend fun updateTransaction(hash: String, status: String, chainId: String) = web3TransactionDao.updateTransaction(hash, status, chainId)

    suspend fun insertWeb3RawTransaction(raw: Web3RawTransaction) = web3RawTransactionDao.insertSuspend(raw)

    fun getPendingTransactionCount(walletId: String): LiveData<Int> = web3TransactionDao.getPendingTransactionCount(walletId)

    suspend fun rampWebUrl(request: RampWebUrlRequest): MixinResponse<RampWebUrlResponse> = routeService.rampWebUrl(request)

    suspend fun getAddressById(chainId: String) = addressDao.getAddressById(chainId)

    suspend fun findTopUsdBalanceAsset(excludeId: String) =
        tokenDao.findTopUsdBalanceAsset(Constants.usdIds, excludeId)

    suspend fun findTopWeb3UsdBalanceAsset(excludeId: String) =
        web3TokenDao.findTopUsdBalanceAsset(Constants.usdIds, excludeId)

    suspend fun getChainItemByWalletId(walletId: String) = web3AddressDao.getChainItemByWalletId(walletId)
}
