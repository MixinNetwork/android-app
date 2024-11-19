@file:Suppress("DEPRECATION")

package one.mixin.android.ui.wallet

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.MIXIN_BOND_USER_ID
import one.mixin.android.Constants.PAGE_SIZE
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.response.ExportRequest
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.crypto.PinCipher
import one.mixin.android.extension.escapeSql
import one.mixin.android.extension.putString
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshTokensJob
import one.mixin.android.job.RefreshTopAssetsJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.tip.TipBody
import one.mixin.android.ui.home.web3.widget.MarketSort
import one.mixin.android.ui.oldwallet.AssetRepository
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.TopAssetItem
import one.mixin.android.vo.User
import one.mixin.android.vo.UtxoItem
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.Output
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.Token
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.sumsub.ProfileResponse
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WalletViewModel
    @Inject
    internal constructor(
        private val userRepository: UserRepository,
        private val accountRepository: AccountRepository,
        private val tokenRepository: TokenRepository,
        private val assetRepository: AssetRepository,
        private val jobManager: MixinJobManager,
        private val pinCipher: PinCipher,
    ) : ViewModel() {
    fun insertUser(user: User) =
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.upsert(user)
        }

    fun assetItemsNotHidden(): LiveData<List<TokenItem>> = tokenRepository.assetItemsNotHidden()

    fun hasAssetsWithValue() = assetRepository.hasAssetsWithValue()

    @ExperimentalPagingApi
    fun snapshots(
        assetId: String,
        type: String? = null,
        otherType: String? = null,
        @Suppress("UNUSED_PARAMETER") initialLoadKey: Int? = 0,
        orderByAmount: Boolean = false,
    ): LiveData<PagingData<SnapshotItem>> =
        tokenRepository.snapshots(assetId, type, otherType, orderByAmount)
            .cachedIn(viewModelScope)

    fun snapshotsLimit(id: String) = tokenRepository.snapshotsLimit(id)
    fun findAddressByReceiver(receiver: String, tag: String) = tokenRepository.findAddressByReceiver(receiver, tag)

    suspend fun snapshotLocal(
        assetId: String,
        snapshotId: String,
    ) = tokenRepository.snapshotLocal(assetId, snapshotId)

    fun assetItem(id: String): LiveData<TokenItem> = tokenRepository.assetItem(id)

    suspend fun simpleAssetItem(id: String) = tokenRepository.simpleAssetItem(id)

    suspend fun verifyPin(code: String) =
        withContext(Dispatchers.IO) {
            accountRepository.verifyPin(code)
        }

    fun checkAndRefreshUsers(userIds: List<String>) =
        viewModelScope.launch {
            val existUsers = userRepository.findUserExist(userIds)
            val queryUsers =
                userIds.filter {
                    !existUsers.contains(it)
                }
            if (queryUsers.isEmpty()) {
                return@launch
            }
            jobManager.addJobInBackground(RefreshUserJob(queryUsers))
        }

    suspend fun updateAssetHidden(
        id: String,
        hidden: Boolean,
    ) = tokenRepository.updateHidden(id, hidden)

    fun hiddenAssets(): LiveData<List<TokenItem>> = tokenRepository.hiddenAssetItems()

    fun addresses(id: String) = tokenRepository.addresses(id)

    fun allSnapshots(
        initialLoadKey: Int? = 0,
        filterParams: FilterParams,
    ): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(
            tokenRepository.allSnapshots(filterParams),
            PagedList.Config.Builder()
                .setPrefetchDistance(PAGE_SIZE * 2)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build(),
        ).setInitialLoadKey(initialLoadKey).build()

    suspend fun allPendingDeposit() = tokenRepository.allPendingDeposit()

    suspend fun refreshPendingDeposits(
        assetId: String,
        depositEntry: DepositEntry,
    ) = tokenRepository.pendingDeposits(assetId, requireNotNull(depositEntry.destination) { "refreshPendingDeposit required destination not null" }, depositEntry.tag)

    suspend fun clearAllPendingDeposits() = tokenRepository.clearAllPendingDeposits()

    suspend fun clearPendingDepositsByAssetId(assetId: String) = tokenRepository.clearPendingDepositsByAssetId(assetId)

    suspend fun insertPendingDeposit(snapshot: List<SafeSnapshot>) = tokenRepository.insertPendingDeposit(snapshot)

    suspend fun getAsset(assetId: String) =
        withContext(Dispatchers.IO) {
            tokenRepository.asset(assetId)
        }

    fun refreshHotAssets() {
        jobManager.addJobInBackground(RefreshTopAssetsJob())
    }

    fun refreshAsset(assetId: String? = null) {
        jobManager.addJobInBackground(RefreshTokensJob(assetId))
    }

    suspend fun queryAsset(query: String): List<TokenItem> = tokenRepository.queryAsset(query)

    fun saveAssets(hotAssetList: List<TopAssetItem>) {
        hotAssetList.forEach {
            jobManager.addJobInBackground(RefreshTokensJob(it.assetId))
        }
    }

    suspend fun findAssetItemById(assetId: String) = tokenRepository.findAssetItemById(assetId)

    suspend fun findOrSyncAsset(
        assetId: String,
    ): TokenItem? {
        return withContext(Dispatchers.IO) {
            tokenRepository.findOrSyncAsset(assetId)
        }
    }

    suspend fun findDepositEntry(chainId: String) = tokenRepository.findDepositEntry(chainId)

    suspend fun findDepositEntryDestinations() = tokenRepository.findDepositEntryDestinations()

    suspend fun findAndSyncDepositEntry(chainId: String) =
        withContext(Dispatchers.IO) {
            tokenRepository.findAndSyncDepositEntry(chainId)
        }

    suspend fun syncDepositEntry(chainId: String) =
        withContext(Dispatchers.IO) {
            tokenRepository.syncDepositEntry(chainId)
        }

    suspend fun syncNoExistAsset(assetIds: List<String>) =
        withContext(Dispatchers.IO) {
            assetIds.forEach { id ->
                if (tokenRepository.findAssetItemById(id) == null) {
                    tokenRepository.findOrSyncAsset(id)
                }
            }
        }

    fun upsetAsset(asset: Token) =
        viewModelScope.launch(Dispatchers.IO) {
            tokenRepository.insert(asset)
        }

    fun observeTopAssets() = tokenRepository.observeTopAssets()

    fun getUser(userId: String) = userRepository.getUserById(userId)

    suspend fun errorCount() = accountRepository.errorCount()

    suspend fun findAssetsByIds(ids: List<String>) = tokenRepository.findAssetsByIds(ids)

    suspend fun assetItems() = tokenRepository.assetItems()

    suspend fun allAssetItems() = tokenRepository.allAssetItems()

    suspend fun fuzzySearchAssets(query: String?): List<TokenItem>? =
        if (query.isNullOrBlank()) {
            null
        } else {
            val escapedQuery = query.trim().escapeSql()
            tokenRepository.fuzzySearchAssetIgnoreAmount(escapedQuery)
        }

    fun updateRecentSearchAssets(
        defaultSharedPreferences: SharedPreferences,
        assetId: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val assetsString =
            defaultSharedPreferences.getString(Constants.Account.PREF_RECENT_SEARCH_ASSETS, null)
        if (assetsString != null) {
            val assetsList = assetsString.split("=")
            if (assetsList.isEmpty()) {
                defaultSharedPreferences.putString(Constants.Account.PREF_RECENT_SEARCH_ASSETS, assetId)
                return@launch
            }

            val arr =
                assetsList.filter { it != assetId }
                    .toMutableList()
                    .also {
                        if (it.size >= Constants.RECENT_SEARCH_ASSETS_MAX_COUNT) {
                            it.dropLast(1)
                        }
                        it.add(0, assetId)
                    }
            defaultSharedPreferences.putString(
                Constants.Account.PREF_RECENT_SEARCH_ASSETS,
                arr.joinToString("="),
            )
        } else {
            defaultSharedPreferences.putString(Constants.Account.PREF_RECENT_SEARCH_ASSETS, assetId)
        }
    }

    suspend fun ticker(
        assetId: String,
        offset: String?,
    ) = tokenRepository.ticker(assetId, offset)

    suspend fun ticker(tickerRequest: RouteTickerRequest): MixinResponse<RouteTickerResponse> =
        tokenRepository.ticker(tickerRequest)

    suspend fun refreshSnapshot(snapshotId: String): SnapshotItem? {
        return withContext(Dispatchers.IO) {
            tokenRepository.refreshAndGetSnapshot(snapshotId)
        }
    }

    suspend fun findSnapshot(snapshotId: String): SnapshotItem? =
        tokenRepository.findSnapshotById(snapshotId)

    suspend fun getFees(
        assetId: String,
        destination: String,
    ) = tokenRepository.getFees(assetId, destination)

    suspend fun profile(): MixinResponse<ProfileResponse> = tokenRepository.profile()

    suspend fun fetchSessionsSuspend(ids: List<String>) = userRepository.fetchSessionsSuspend(ids)

    suspend fun findBotPublicKey(
        conversationId: String,
        botId: String,
    ) = userRepository.findBotPublicKey(conversationId, botId)

    suspend fun saveSession(participantSession: ParticipantSession) {
        userRepository.saveSession(participantSession)
    }

    suspend fun deleteSessionByUserId(
        conversationId: String,
        userId: String,
    ) =
        withContext(Dispatchers.IO) {
            userRepository.deleteSessionByUserId(conversationId, userId)
        }

    fun insertDeposit(data: List<DepositEntry>) {
        tokenRepository.insertDeposit(data)
    }

    suspend fun checkHasOldAsset(): Boolean {
        return handleMixinResponse(
            invokeNetwork = {
                tokenRepository.findOldAssets()
            },
            successBlock = {
                return@handleMixinResponse it.data?.any { asset ->
                    BigDecimal(asset.balance) != BigDecimal.ZERO
                } ?: false
            },
        ) ?: false
    }

    suspend fun findBondBotUrl() = userRepository.findOrSyncApp(MIXIN_BOND_USER_ID)

    fun utxoItem(asset: String): LiveData<PagingData<UtxoItem>> {
        return tokenRepository.utxoItem(asset)
    }

    suspend fun removeUtxo(outputId: String) = tokenRepository.removeUtxo(outputId)

    suspend fun refreshUser(userId: String) = userRepository.refreshUser(userId)

    suspend fun findLatestOutputSequenceByAsset(asset: String) = tokenRepository.findLatestOutputSequenceByAsset(asset)

    suspend fun insertOutputs(outputs: List<Output>) = withContext(SINGLE_DB_THREAD) { tokenRepository.insertOutputs(outputs) }

    suspend fun deleteByKernelAssetIdAndOffset(
        kernelAssetId: String,
        offset: Long,
    ) = tokenRepository.deleteByKernelAssetIdAndOffset(kernelAssetId, offset)

    suspend fun getOutputs(
        members: String,
        threshold: Int,
        offset: Long? = null,
        limit: Int = 500,
        state: String? = null,
        asset: String? = null,
    ) = tokenRepository.getOutputs(
        members,
        threshold,
        offset,
        limit,
        state,
        asset,
    )

    suspend fun priceHistory(
        assetId: String,
        type: String,
    ) = tokenRepository.priceHistory(assetId, type)

    fun marketById(assetId: String) = tokenRepository.marketById(assetId)

    fun marketByCoinId(coinId: String) = tokenRepository.marketByCoinId(coinId)

    fun historyPriceById(assetId: String) = tokenRepository.historyPriceById(assetId)

    fun getWeb3Markets(limit: Int, sort: MarketSort):
        Flow<PagingData<MarketItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance =10,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { tokenRepository.getWeb3Markets(limit, sort) }
        ).flow.cachedIn(viewModelScope)
    }

    fun getFavoredWeb3Markets(sort: MarketSort): Flow<PagingData<MarketItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance =10,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { tokenRepository.getFavoredWeb3Markets(sort) }
        ).flow
    }

    suspend fun findTokensByCoinId(coinId: String): List<TokenItem> = tokenRepository.findTokensByCoinId(coinId)

    suspend fun findTokenIdsByCoinId(coinId: String) = tokenRepository.findTokenIdsByCoinId(coinId)

    suspend fun findMarketItemByAssetId(assetId: String) = tokenRepository.findMarketItemByAssetId(assetId)

    fun updateMarketFavored(symbol: String, coinId: String, isFavored: Boolean?) = viewModelScope.launch(Dispatchers.IO) { tokenRepository.updateMarketFavored(symbol, coinId, isFavored) }

    suspend fun simpleCoinItem(coinId: String) = tokenRepository.simpleCoinItem(coinId)

    suspend fun simpleCoinItemByAssetId(assetId: String) = tokenRepository.simpleCoinItemByAssetId(assetId)

    fun anyAlertByCoinId(coinId: String) = tokenRepository.anyAlertByCoinId(coinId)

    fun anyAlertByAssetId(assetId: String) = tokenRepository.anyAlertByAssetId(assetId)

    suspend fun refreshMarket(
        coinId: String, endBlock: () -> Unit, failureBlock: (suspend (MixinResponse<Market>) -> Boolean),
        exceptionBlock: (suspend (t: Throwable) -> Boolean)
    ) = tokenRepository.refreshMarket(coinId, endBlock, failureBlock, exceptionBlock)

    suspend fun saltExport(exportRequest: ExportRequest) = accountRepository.saltExport(exportRequest)

    suspend fun getEncryptedTipBody(
        userId: String,
        pin: String,
    ): String = pinCipher.encryptPin(pin, TipBody.forExport(userId))

}