package one.mixin.android.repository

import TransactionResponse
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
import one.mixin.android.api.request.WithdrawalRequest
import one.mixin.android.api.response.RoutePaymentResponse
import one.mixin.android.api.response.RouteSessionResponse
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.api.service.AddressService
import one.mixin.android.api.service.AssetService
import one.mixin.android.api.service.TokenService
import one.mixin.android.api.service.RouteService
import one.mixin.android.api.service.UtxoService
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.crypto.verifyCurve25519Signature
import one.mixin.android.db.AddressDao
import one.mixin.android.db.TokenDao
import one.mixin.android.db.TokensExtraDao
import one.mixin.android.db.ChainDao
import one.mixin.android.db.DepositDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.OutputDao
import one.mixin.android.db.RawTransactionDao
import one.mixin.android.db.SafeSnapshotDao
import one.mixin.android.db.TopAssetDao
import one.mixin.android.db.TraceDao
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.db.provider.DataProvider
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.within6Hours
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.wallet.adapter.SnapshotsMediator
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.ErrorHandler.Companion.NOT_FOUND
import one.mixin.android.vo.Address
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.Card
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.safe.Output
import one.mixin.android.vo.PriceAndChange
import one.mixin.android.vo.SafeBox
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import one.mixin.android.vo.safe.Token
import one.mixin.android.vo.safe.TokensExtra
import one.mixin.android.vo.Trace
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.route.RoutePaymentRequest
import one.mixin.android.vo.safe.toAssetItem
import one.mixin.android.vo.safe.toPriceAndChange
import one.mixin.android.vo.sumsub.ProfileResponse
import one.mixin.android.vo.sumsub.RouteTokenResponse
import one.mixin.android.vo.safe.RawTransaction
import retrofit2.Call
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
    private val assetsExtraDao: TokensExtraDao,
    private val safeSnapshotDao: SafeSnapshotDao,
    private val addressDao: AddressDao,
    private val addressService: AddressService,
    private val hotAssetDao: TopAssetDao,
    private val traceDao: TraceDao,
    private val chainDao: ChainDao,
    private val depositDao: DepositDao,
    private val rawTransactionDao: RawTransactionDao,
    private val outputDao: OutputDao,
    private val jobManager: MixinJobManager,
    private val safeBox: DataStore<SafeBox>,
) {

    fun assets() = tokenService.assets()

    suspend fun simpleAssetsWithBalance() = tokenDao.simpleAssetsWithBalance()

    fun insert(asset: Token) {
        tokenDao.insert(asset)
    }

    fun insertList(asset: List<Token>) {
        tokenDao.insertList(asset)
    }

    suspend fun asset(id: String) = tokenService.getAssetByIdSuspend(id)

    suspend fun getAssetPrecisionById(id: String) = tokenService.getAssetPrecisionById(id)

    suspend fun findOrSyncAsset(assetId: String): TokenItem? {
        var assetItem = tokenDao.findAssetItemById(assetId)
        if (assetItem != null && !assetItem.getDestination().isNullOrBlank()) {
            return assetItem
        } else if (assetItem != null) {
            val userId = requireNotNull(Session.getAccountId())
            handleMixinResponse(
                invokeNetwork = {
                    utxoService.createDeposit(
                        DepositEntryRequest(assetItem!!.chainId)
                    )
                },
                successBlock = { resp ->
                    val pub = SAFE_PUBLIC_KEY.hexStringToByteArray()
                    resp.data?.filter {
                        val message = if (it.tag.isNullOrBlank()) {
                            it.destination
                        } else {
                            "${it.destination}:${it.tag}"
                        }.toByteArray().sha3Sum256()
                        val signature = it.signature.hexStringToByteArray()
                        verifyCurve25519Signature(message, signature, pub)
                    }?.let { list ->
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

    suspend fun syncAsset(assetId: String): TokenItem? {
        val asset: Token = handleMixinResponse(
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
            config = PagingConfig(
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
            remoteMediator = SnapshotsMediator(
                tokenService,
                safeSnapshotDao,
                tokenDao,
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
        }
    }

    suspend fun snapshotLocal(assetId: String, snapshotId: String) =
        safeSnapshotDao.snapshotLocal(assetId, snapshotId)

    fun insertSnapshot(snapshot: SafeSnapshot) = safeSnapshotDao.insert(snapshot)

    fun getXIN() = tokenDao.getXIN()

    suspend fun paySuspend(request: TransferRequest) = tokenService.paySuspend(request)

    suspend fun updateHidden(id: String, hidden: Boolean) {
        appDatabase.withTransaction {
            val assetsExtra = assetsExtraDao.findByAssetId(id)
            if (assetsExtra != null) {
                assetsExtraDao.updateHiddenByAssetId(id, hidden)
            } else {
                assetsExtraDao.insertSuspend(TokensExtra(id, assetIdToAsset(id), hidden, "0", nowInUtc()))
            }
        }
    }

    fun hiddenAssetItems() = tokenDao.hiddenAssetItems()

    fun addresses(id: String) = addressDao.addresses(id)

    fun observeAddress(addressId: String) = addressDao.observeById(addressId)

    suspend fun withdrawal(withdrawalRequest: WithdrawalRequest) =
        tokenService.withdrawals(withdrawalRequest)

    fun saveAddr(addr: Address) = addressDao.insert(addr)

    suspend fun syncAddr(addressRequest: AddressRequest) = addressService.addresses(addressRequest)

    suspend fun deleteAddr(id: String, pin: String) = addressService.delete(id, Pin(pin))

    suspend fun deleteLocalAddr(id: String) = addressDao.deleteById(id)

    fun assetItemsNotHidden() = tokenDao.assetItemsNotHidden()

    fun assetItems() = tokenDao.assetItems()

    fun assetItems(assetIds: List<String>) = tokenDao.assetItems(assetIds)

    suspend fun fuzzySearchToken(query: String, cancellationSignal: CancellationSignal) =
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

    suspend fun pendingDeposits(asset: String, destination: String, tag: String? = null) =
        tokenService.pendingDeposits(asset, destination, tag)

    suspend fun clearPendingDepositsByAssetId(assetId: String) =
        safeSnapshotDao.clearPendingDepositsByAssetId(assetId)

    suspend fun queryAsset(query: String): List<TokenItem> {
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
            val tokenItemList = arrayListOf<TokenItem>()
            assetList.mapTo(tokenItemList) { asset ->
                var chainIconUrl = getIconUrl(asset.chainId)
                if (chainIconUrl == null) {
                    chainIconUrl = fetchAsset(asset.chainId)
                }
                // todo check asset balance
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

    private suspend fun queryAssets(query: String) = tokenService.queryAssets(query)

    private suspend fun getIconUrl(id: String) = tokenDao.getIconUrl(id)

    fun observeTopAssets() = hotAssetDao.topAssets()

    fun checkExists(id: String) = tokenDao.checkExists(id)

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

    suspend fun getSnapshots(
        assetId: String,
        offset: String?,
        limit: Int,
        opponent: String?,
        destination: String?,
        tag: String?
    ) =
        tokenService.getSnapshots(assetId, offset, limit, opponent, destination, tag)

    suspend fun insertTrace(trace: Trace) = traceDao.insertSuspend(trace)

    suspend fun suspendFindTraceById(traceId: String): Trace? =
        traceDao.suspendFindTraceById(traceId)

    suspend fun getTrace(traceId: String) = tokenService.getTrace(traceId)

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
                    tokenService.getTrace(trace.traceId)
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

    suspend fun ticker(assetId: String, offset: String?) = tokenService.ticker(assetId, offset)

    suspend fun ticker(tickerRequest: RouteTickerRequest): MixinResponse<RouteTickerResponse> =
        routeService.ticker(tickerRequest)

    suspend fun findSnapshotByTransactionHashList(
        assetId: String,
        hashList: List<String>
    ): List<String> =
        safeSnapshotDao.findSnapshotIdsByTransactionHashList(assetId, hashList)

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

    suspend fun deleteInstruments(id: String): MixinResponse<Void> = routeService.deleteInstruments(id)

    suspend fun transactionRequest(transactionRequest: TransactionRequest) = utxoService.transactionRequest(transactionRequest)

    suspend fun transactions(transactionRequest: TransactionRequest) = utxoService.transactions(transactionRequest)

    suspend fun ghostKey(ghostKeyRequest: List<GhostKeyRequest>) = utxoService.ghostKey(ghostKeyRequest)

    suspend fun findOutputs(limit: Int, asset: String) = outputDao.findUnspentOutputsByAsset(limit, asset)

    fun insertOutput(output: Output) = outputDao.insert(output)

    fun insertDeposit(data: List<DepositEntry>) {
        depositDao.insertList(data)
    }

    fun insetRawTransaction(rawTransaction: RawTransaction) {
        rawTransactionDao.insert(rawTransaction)
    }

    fun deleteRawTransaction(requestId: String) {
        rawTransactionDao.deleteById(requestId)
    }

    fun signed(hash: List<String>) {
        outputDao.signedUtxo(hash)
    }

    suspend fun findOldAssets() = assetService.fetchAllAssetSuspend()
    fun insertSnapshotMessage(data: TransactionResponse, conversationId:String ,assetId: String, amount: String, opponentId: String, memo: String?) {
        val snapshotId =  UUID.nameUUIDFromBytes("${data.userId}:${data.transactionHash}".toByteArray()).toString()
        val snapshot = SafeSnapshot(snapshotId, SnapshotType.transfer.name, assetId, "-${amount}", data.snapshotAt, opponentId, null, null, null, null, memo, null, null, null, null)
        val message = createMessage(UUID.randomUUID().toString(), conversationId, data.userId, MessageCategory.SYSTEM_SAFE_SNAPSHOT.name, "", data.createdAt, MessageStatus.DELIVERED.name, snapshot.type, null, snapshot.snapshotId)
        safeSnapshotDao.insert(snapshot)
        appDatabase.insertMessage(message)
        MessageFlow.insert(message.conversationId, message.messageId)
    }
}