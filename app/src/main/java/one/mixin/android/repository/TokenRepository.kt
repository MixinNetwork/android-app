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
import one.mixin.android.BuildConfig.VERSION_NAME
import one.mixin.android.Constants
import one.mixin.android.Constants.SAFE_PUBLIC_KEY
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.DepositEntryRequest
import one.mixin.android.api.request.GhostKeyRequest
import one.mixin.android.api.request.Pin
import one.mixin.android.api.request.RouteInstrumentRequest
import one.mixin.android.api.request.RouteSessionRequest
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.request.RouteTokenRequest
import one.mixin.android.api.request.TransactionRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.response.RoutePaymentResponse
import one.mixin.android.api.response.RouteSessionResponse
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.api.response.TransactionResponse
import one.mixin.android.api.service.AddressService
import one.mixin.android.api.service.AssetService
import one.mixin.android.api.service.RouteService
import one.mixin.android.api.service.TokenService
import one.mixin.android.api.service.UtxoService
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.crypto.verifyCurve25519Signature
import one.mixin.android.db.AddressDao
import one.mixin.android.db.ChainDao
import one.mixin.android.db.DepositDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.OutputDao
import one.mixin.android.db.RawTransactionDao
import one.mixin.android.db.SafeSnapshotDao
import one.mixin.android.db.TokenDao
import one.mixin.android.db.TokensExtraDao
import one.mixin.android.db.TopAssetDao
import one.mixin.android.db.TraceDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.db.provider.DataProvider
import one.mixin.android.db.runInTransaction
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.toHex
import one.mixin.android.extension.within6Hours
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.wallet.adapter.SnapshotsMediator
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.ErrorHandler.Companion.NOT_FOUND
import one.mixin.android.vo.Address
import one.mixin.android.vo.Card
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.PriceAndChange
import one.mixin.android.vo.SafeBox
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.Trace
import one.mixin.android.vo.UtxoItem
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.route.RoutePaymentRequest
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.Output
import one.mixin.android.vo.safe.RawTransaction
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.SafeSnapshotType
import one.mixin.android.vo.safe.SafeWithdrawal
import one.mixin.android.vo.safe.Token
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.TokensExtra
import one.mixin.android.vo.safe.toAssetItem
import one.mixin.android.vo.safe.toPriceAndChange
import one.mixin.android.vo.sumsub.ProfileResponse
import one.mixin.android.vo.sumsub.RouteTokenResponse
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
        private val jobManager: MixinJobManager,
        private val safeBox: DataStore<SafeBox>,
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
                assetItem.chainPriceUsd = chain?.chainPriceUsd
            }
            return assetItem
        }

        suspend fun syncDepositEntry(chainId: String): Pair<DepositEntry?, Int> {
            var code = 200
            val depositEntry =
                handleMixinResponse(
                    invokeNetwork = {
                        utxoService.createDeposit(
                            DepositEntryRequest(chainId),
                        )
                    },
                    failureBlock = {
                        code = it.errorCode
                        code == ErrorHandler.ADDRESS_GENERATING
                    },
                    successBlock = { resp ->
                        val pub = SAFE_PUBLIC_KEY.hexStringToByteArray()
                        resp.data?.filter {
                            val message =
                                if (it.tag.isNullOrBlank()) {
                                    it.destination
                                } else {
                                    "${it.destination}:${it.tag}"
                                }.toByteArray().sha3Sum256()
                            val signature = it.signature.hexStringToByteArray()
                            verifyCurve25519Signature(message, signature, pub)
                        }?.let { list ->
                            runInTransaction {
                                depositDao.deleteByChainId(chainId)
                                depositDao.insertList(list)
                            }
                            list.find { it.isPrimary }
                        }
                    },
                )
            return Pair(depositEntry, code)
        }

        suspend fun findDepositEntry(chainId: String) = depositDao.findDepositEntry(chainId)

        suspend fun findDepositEntryDestinations() = depositDao.findDepositEntryDestinations()

        suspend fun findAndSyncDepositEntry(chainId: String): Triple<DepositEntry?, Boolean, Int> {
            val oldDeposit = depositDao.findDepositEntry(chainId)
            val (newDeposit, code) = syncDepositEntry(chainId)
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

        private suspend fun simpleAsset(id: String) = tokenDao.simpleAsset(id)

        suspend fun insertPendingDeposit(snapshot: List<SafeSnapshot>) = safeSnapshotDao.insertListSuspend(snapshot)

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

        fun snapshotsFromDb(
            id: String,
            type: String? = null,
            otherType: String? = null,
            orderByAmount: Boolean = false,
        ): DataSource.Factory<Int, SnapshotItem> {
            return if (type == null) {
                if (orderByAmount) {
                    safeSnapshotDao.snapshotsOrderByAmount(id)
                } else {
                    safeSnapshotDao.snapshots(id)
                }
            } else {
                if (orderByAmount) {
                    safeSnapshotDao.snapshotsByTypeOrderByAmount(id, type, otherType)
                } else {
                    safeSnapshotDao.snapshotsByType(id, type, otherType)
                }
            }.map {
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

        suspend fun snapshotLocal(
            assetId: String,
            snapshotId: String,
        ) =
            safeSnapshotDao.snapshotLocal(assetId, snapshotId)

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

        fun assetItems(assetIds: List<String>) = tokenDao.assetItems(assetIds)

        suspend fun findTokenItems(ids: List<String>): List<TokenItem> = tokenDao.findTokenItems(ids)

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

        fun allSnapshots(
            type: String? = null,
            otherType: String? = null,
            orderByAmount: Boolean = false,
        ): DataSource.Factory<Int, SnapshotItem> {
            return if (type == null) {
                if (orderByAmount) {
                    safeSnapshotDao.allSnapshotsOrderByAmount()
                } else {
                    safeSnapshotDao.allSnapshots()
                }
            } else {
                if (orderByAmount) {
                    safeSnapshotDao.allSnapshotsByTypeOrderByAmount(type, otherType)
                } else {
                    safeSnapshotDao.allSnapshotsByType(type, otherType)
                }
            }
        }

        fun snapshotsByUserId(opponentId: String) = safeSnapshotDao.snapshotsByUserId(opponentId)

        suspend fun allPendingDeposit() = tokenService.allPendingDeposits()

        suspend fun pendingDeposits(
            asset: String,
            destination: String,
            tag: String? = null,
        ) =
            tokenService.pendingDeposits(asset, destination, tag)

        suspend fun clearAllPendingDeposits() = safeSnapshotDao.clearAllPendingDeposits()

        suspend fun clearPendingDepositsByAssetId(assetId: String) =
            safeSnapshotDao.clearPendingDepositsByAssetId(assetId)

        suspend fun queryAsset(query: String): List<TokenItem> {
            val response =
                try {
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
                    tokenItemList
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

        private suspend fun queryAssets(query: String) = tokenService.queryAssets(query)

        private suspend fun getIconUrl(id: String) = tokenDao.getIconUrl(id)

        fun observeTopAssets() = hotAssetDao.topAssets()

        fun checkExists(id: String) = tokenDao.checkExists(id)

        suspend fun findAddressById(
            addressId: String,
            assetId: String,
        ) =
            addressDao.findAddressById(addressId, assetId)

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

        suspend fun findAssetItemById(assetId: String) = tokenDao.findAssetItemById(assetId)

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

        suspend fun deleteInstruments(id: String): MixinResponse<Void> = routeService.deleteInstruments(id)

        suspend fun transactionRequest(transactionRequests: List<TransactionRequest>) = utxoService.transactionRequest(transactionRequests)

        suspend fun transactions(transactionRequests: List<TransactionRequest>) = utxoService.transactions(transactionRequests)

        suspend fun ghostKey(ghostKeyRequest: List<GhostKeyRequest>) = utxoService.ghostKey(ghostKeyRequest)

        suspend fun findOutputs(
            limit: Int,
            asset: String,
        ) = outputDao.findUnspentOutputsByAsset(limit, asset)

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
        ) {
            val snapshotId = data.getSnapshotId
            if (conversationId != "") {
                val message = createMessage(UUID.randomUUID().toString(), conversationId, data.userId, MessageCategory.SYSTEM_SAFE_SNAPSHOT.name, "", data.createdAt, MessageStatus.DELIVERED.name, SafeSnapshotType.snapshot.name, null, snapshotId)
                appDatabase.insertMessage(message)
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
        ) {
            val snapshot = SafeSnapshot(snapshotId, type.name, assetId, "-$amount", userId, opponentId, memo?.toHex() ?: "", transactionHash, nowInUtc(), requestId, null, null, null, null, withdrawal)
            safeSnapshotDao.insert(snapshot)
        }

        fun findUser(userId: String) = userDao.findUser(userId)

        fun findRawTransaction(traceId: String) = rawTransactionDao.findRawTransaction(traceId)

        suspend fun getFees(
            id: String,
            destination: String,
        ) = tokenService.getFees(id, destination)

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

        fun firstUnspentTransaction() = rawTransactionDao.findUnspentTransaction()

        fun find30daysWithdrawByAddress(formatDestination: String) = rawTransactionDao.find30daysWithdrawByAddress(formatDestination)

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
    }
