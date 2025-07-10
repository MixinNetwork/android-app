@file:Suppress("DEPRECATION")

package one.mixin.android.ui.wallet

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.MIXIN_BOND_USER_ID
import one.mixin.android.Constants.PAGE_SIZE
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.response.ExportRequest
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.crypto.PinCipher
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.escapeSql
import one.mixin.android.extension.putString
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshTokensJob
import one.mixin.android.job.RefreshTopAssetsJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.tip.TipBody
import one.mixin.android.ui.conversation.holder.TimeBubble
import one.mixin.android.ui.home.web3.widget.MarketSort
import one.mixin.android.ui.oldwallet.AssetRepository
import one.mixin.android.ui.wallet.components.WalletDestination
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
import one.mixin.android.db.web3.vo.Web3Wallet
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WalletViewModel
@Inject
internal constructor(
    private val walletDatabase: WalletDatabase,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val web3Repository: Web3Repository,
    private val tokenRepository: TokenRepository,
    private val assetRepository: AssetRepository,
    private val jobManager: MixinJobManager,
    private val pinCipher: PinCipher,
    private val defaultSharedPreferences: SharedPreferences,
) : ViewModel() {

    private val _walletsFlow = MutableStateFlow<List<Web3Wallet>>(emptyList())
    val walletsFlow: StateFlow<List<Web3Wallet>> = _walletsFlow

    fun searchWallets(excludeWalletId: String, query: String) {
        viewModelScope.launch {
            _walletsFlow.value = getWalletsExcluding(excludeWalletId, query)
        }
    }

    private val _selectedWalletId = MutableStateFlow<String?>(null)
    val selectedWalletId: StateFlow<String?> = _selectedWalletId.asStateFlow()

    private val _selectedWalletDestination = MutableLiveData<WalletDestination>()
    val selectedWalletDestination: LiveData<WalletDestination> = _selectedWalletDestination

    private val _hasUsedWallet = MutableLiveData<Boolean>()
    val hasUsedWallet: LiveData<Boolean> = _hasUsedWallet

    val selectedWallet: LiveData<TokenItem?> = _selectedWalletDestination.switchMap { dest ->
        liveData {
            emit(loadWalletAsset(dest))
        }
    }

    init {
        initializeWallet()
    }

    private fun initializeWallet() {
        viewModelScope.launch(Dispatchers.IO) {
            val walletPref = defaultSharedPreferences.getString(Constants.Account.PREF_HAS_USED_WALLET, null)
            withContext(Dispatchers.Main) {
                _hasUsedWallet.value = walletPref != null
            }
            val walletDestination = WalletDestination.fromString(walletPref)
            withContext(Dispatchers.Main) {
                _selectedWalletDestination.value = walletDestination
            }
        }
    }

    fun selectWallet(walletDestination: WalletDestination) {
        viewModelScope.launch(Dispatchers.IO) {
            defaultSharedPreferences.putString(Constants.Account.PREF_HAS_USED_WALLET, walletDestination.toString())
            withContext(Dispatchers.Main) {
                _hasUsedWallet.value = true
                _selectedWalletDestination.value = walletDestination
            }
        }
    }

    private suspend fun loadWalletAsset(walletDestination: WalletDestination): TokenItem? {
        return when (walletDestination) {
            is WalletDestination.Privacy -> null
            is WalletDestination.Classic -> tokenRepository.simpleAssetItem(walletDestination.walletId)
            is WalletDestination.Import -> tokenRepository.simpleAssetItem(walletDestination.walletId)
        }
    }

    fun setSelectedWallet(walletId: String?) {
        _selectedWalletId.value = walletId
        Timber.d("Selected wallet changed to: $walletId")
    }

    fun getCurrentSelectedWalletId(): String? {
        return _selectedWalletId.value
    }

    fun clearSelectedWallet() {
        setSelectedWallet(null)
    }

    fun insertUser(user: User) =
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.upsert(user)
        }

    suspend fun  web3TokenItemById(chainId: String) = web3Repository.web3TokenItemById(chainId)

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

    fun allWeb3Transaction(
        initialLoadKey: Int? = 0,
        filterParams: Web3FilterParams,
    ): LiveData<PagedList<Web3TransactionItem>> =
        LivePagedListBuilder(
            tokenRepository.allWeb3Transaction(filterParams),
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

    fun getPendingDisplays() = tokenRepository.getPendingDisplays()

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

    suspend fun queryAsset(walletId: String?, query: String, web3: Boolean = false): List<TokenItem> = tokenRepository.queryAsset(walletId, query, web3)

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

    suspend fun findAndSyncDepositEntry(chainId: String, assetId: String?) =
        withContext(Dispatchers.IO) {
            tokenRepository.findAndSyncDepositEntry(chainId, assetId)
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

    suspend fun searchAssetsByAddresses(addresses: List<String>) = web3Repository.searchAssetsByAddresses(addresses)

    suspend fun renameWallet(walletId: String, newName: String) {
        withContext(Dispatchers.IO) {
            try {
                val request = WalletRequest(name = newName, category = null, addresses = null)
                val response = web3Repository.updateWallet(walletId, request)
                if (response.isSuccess && response.data != null) {
                    // Update local database
                    web3Repository.updateWalletName(walletId, newName)
                    Timber.d("Successfully renamed wallet $walletId to $newName")
                } else {
                    Timber.e("Failed to rename wallet: ${response.errorCode} - ${response.errorDescription}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to rename wallet $walletId")
            }
        }
    }

    suspend fun deleteWallet(walletId: String) {
        withContext(Dispatchers.IO) {
            try {
                val response = web3Repository.destroyWallet(walletId)
                if (response.isSuccess) {
                    web3Repository.deleteTransactionsByWalletId(walletId)
                    web3Repository.deleteAddressesByWalletId(walletId)
                    web3Repository.deleteAssetsByWalletId(walletId)
                    web3Repository.deleteWallet(walletId)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    suspend fun findWalletById(walletId: String) = web3Repository.findWalletById(walletId)

    suspend fun getWalletsExcluding(excludeWalletId: String, query: String) = web3Repository.getWalletsExcluding(excludeWalletId, query)
}
