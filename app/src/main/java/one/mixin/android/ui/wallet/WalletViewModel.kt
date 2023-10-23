@file:Suppress("DEPRECATION")

package one.mixin.android.ui.wallet

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.PAGE_SIZE
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.extension.escapeSql
import one.mixin.android.extension.putString
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.job.RefreshTopAssetsJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.TokenItem
import one.mixin.android.vo.Deposit
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.SafeSnapshot
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.Token
import one.mixin.android.vo.TopAssetItem
import one.mixin.android.vo.User
import one.mixin.android.vo.sumsub.ProfileResponse
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WalletViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val assetRepository: AssetRepository,
    private val jobManager: MixinJobManager,
) : ViewModel() {

    fun insertUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
        userRepository.upsert(user)
    }

    fun assetItemsNotHidden(): LiveData<List<TokenItem>> = assetRepository.assetItemsNotHidden()

    @ExperimentalPagingApi
    fun snapshots(
        assetId: String,
        type: String? = null,
        otherType: String? = null,
        @Suppress("UNUSED_PARAMETER") initialLoadKey: Int? = 0,
        orderByAmount: Boolean = false,
    ): LiveData<PagingData<SnapshotItem>> =
        assetRepository.snapshots(assetId, type, otherType, orderByAmount)
            .cachedIn(viewModelScope)

    fun snapshotsFromDb(
        id: String,
        type: String? = null,
        otherType: String? = null,
        initialLoadKey: Int? = 0,
        orderByAmount: Boolean = false,
    ): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(
            assetRepository.snapshotsFromDb(id, type, otherType, orderByAmount),
            PagedList.Config.Builder()
                .setPrefetchDistance(PAGE_SIZE)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build(),
        )
            .setInitialLoadKey(initialLoadKey)
            .build()

    fun snapshotsByUserId(
        opponentId: String,
        initialLoadKey: Int? = 0,
    ): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(
            assetRepository.snapshotsByUserId(opponentId),
            PagedList.Config.Builder()
                .setPrefetchDistance(PAGE_SIZE)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build(),
        )
            .setInitialLoadKey(initialLoadKey)
            .build()

    suspend fun snapshotLocal(assetId: String, snapshotId: String) = assetRepository.snapshotLocal(assetId, snapshotId)

    fun assetItem(id: String): LiveData<TokenItem> = assetRepository.assetItem(id)

    suspend fun simpleAssetItem(id: String) = assetRepository.simpleAssetItem(id)

    suspend fun verifyPin(code: String) = withContext(Dispatchers.IO) {
        accountRepository.verifyPin(code)
    }

    fun checkAndRefreshUsers(userIds: List<String>) = viewModelScope.launch {
        val existUsers = userRepository.findUserExist(userIds)
        val queryUsers = userIds.filter {
            !existUsers.contains(it)
        }
        if (queryUsers.isEmpty()) {
            return@launch
        }
        jobManager.addJobInBackground(RefreshUserJob(queryUsers))
    }

    suspend fun updateAssetHidden(id: String, hidden: Boolean) = assetRepository.updateHidden(id, hidden)

    fun hiddenAssets(): LiveData<List<TokenItem>> = assetRepository.hiddenAssetItems()

    fun addresses(id: String) = assetRepository.addresses(id)

    fun allSnapshots(type: String? = null, otherType: String? = null, initialLoadKey: Int? = 0, orderByAmount: Boolean = false): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(
            assetRepository.allSnapshots(type, otherType, orderByAmount = orderByAmount),
            PagedList.Config.Builder()
                .setPrefetchDistance(PAGE_SIZE * 2)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build(),
        )
            .setInitialLoadKey(initialLoadKey)
            .build()

    suspend fun getAsset(assetId: String) = withContext(Dispatchers.IO) {
        assetRepository.asset(assetId)
    }

    fun refreshHotAssets() {
        jobManager.addJobInBackground(RefreshTopAssetsJob())
    }

    fun refreshAsset(assetId: String? = null) {
        jobManager.addJobInBackground(RefreshAssetsJob(assetId))
    }

    suspend fun queryAsset(query: String): List<TokenItem> = assetRepository.queryAsset(query)

    fun saveAssets(hotAssetList: List<TopAssetItem>) {
        hotAssetList.forEach {
            jobManager.addJobInBackground(RefreshAssetsJob(it.assetId))
        }
    }

    suspend fun findAssetItemById(assetId: String) = assetRepository.findAssetItemById(assetId)

    suspend fun findOrSyncAsset(assetId: String): TokenItem? {
        return withContext(Dispatchers.IO) {
            assetRepository.findOrSyncAsset(assetId)
        }
    }

    suspend fun createDeposit(chinaId: String, assetId: String): MixinResponse<List<Deposit>> {
        return withContext(Dispatchers.IO) {
            assetRepository.createDeposit(chinaId)
        }
    }

    suspend fun syncNoExistAsset(assetIds: List<String>) = withContext(Dispatchers.IO) {
        assetIds.forEach { id ->
            if (assetRepository.findAssetItemById(id) == null) {
                assetRepository.findOrSyncAsset(id)
            }
        }
    }

    fun upsetAsset(asset: Token) = viewModelScope.launch(Dispatchers.IO) {
        assetRepository.insert(asset)
    }

    fun observeTopAssets() = assetRepository.observeTopAssets()

    fun getUser(userId: String) = userRepository.getUserById(userId)

    suspend fun errorCount() = accountRepository.errorCount()

    fun refreshSnapshots(
        assetId: String? = null,
        offset: String? = null,
        opponent: String? = null,
    ) {
        jobManager.addJobInBackground(RefreshSnapshotsJob(assetId, offset, opponent))
    }

    suspend fun getSnapshots(assetId: String, offset: String?, limit: Int, opponent: String?, destination: String?, tag: String?) =
        assetRepository.getSnapshots(
            assetId,
            offset,
            limit,
            opponent,
            destination,
            if (tag?.isEmpty() == true) {
                null
            } else {
                tag
            },
        )

    suspend fun findAssetsByIds(ids: List<String>) = assetRepository.findAssetsByIds(ids)

    suspend fun assetItems() = assetRepository.assetItems()

    suspend fun fuzzySearchAssets(query: String?): List<TokenItem>? =
        if (query.isNullOrBlank()) {
            null
        } else {
            val escapedQuery = query.trim().escapeSql()
            assetRepository.fuzzySearchAssetIgnoreAmount(escapedQuery)
        }

    fun updateRecentSearchAssets(
        defaultSharedPreferences: SharedPreferences,
        assetId: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val assetsString =
            defaultSharedPreferences.getString(Constants.Account.PREF_RECENT_SEARCH_ASSETS, null)
        if (assetsString != null) {
            val assetsList = assetsString.split("=")
            if (assetsList.isNullOrEmpty()) {
                defaultSharedPreferences.putString(Constants.Account.PREF_RECENT_SEARCH_ASSETS, assetId)
                return@launch
            }

            val arr = assetsList.filter { it != assetId }
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

    suspend fun ticker(assetId: String, offset: String?) = assetRepository.ticker(assetId, offset)

    suspend fun ticker(tickerRequest: RouteTickerRequest): MixinResponse<RouteTickerResponse> =
        assetRepository.ticker(tickerRequest)

    suspend fun refreshSnapshot(snapshotId: String): SnapshotItem? {
        return withContext(Dispatchers.IO) {
            assetRepository.refreshAndGetSnapshot(snapshotId)
        }
    }

    suspend fun findSnapshot(snapshotId: String): SnapshotItem? =
        assetRepository.findSnapshotById(snapshotId)

    suspend fun getExternalAddressFee(assetId: String, destination: String, tag: String?) =
        accountRepository.getExternalAddressFee(assetId, destination, tag)

    suspend fun profile(): MixinResponse<ProfileResponse> = assetRepository.profile()

    suspend fun fetchSessionsSuspend(ids: List<String>) = userRepository.fetchSessionsSuspend(ids)
    suspend fun findBotPublicKey(conversationId: String, botId: String) = userRepository.findBotPublicKey(conversationId, botId)
    suspend fun saveSession(participantSession: ParticipantSession) {
        userRepository.saveSession(participantSession)
    }

    suspend fun deleteSessionByUserId(conversationId: String, userId: String) = withContext(Dispatchers.IO) {
        userRepository.deleteSessionByUserId(conversationId, userId)
    }

    fun insertDeposit(data: List<Deposit>) {
        assetRepository.insertDeposit(data)
    }

    suspend fun checkHasOldAsset(): Boolean {
       return handleMixinResponse(
            invokeNetwork = {
                assetRepository.findOldAssets()
            },
            successBlock = {
                return@handleMixinResponse it.data?.any { asset ->
                    BigDecimal(asset.balance) != BigDecimal.ZERO
                } ?: false
            },
        ) ?: false
    }
}
