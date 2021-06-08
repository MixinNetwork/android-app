package one.mixin.android.ui.wallet

import android.content.SharedPreferences
import androidx.collection.ArraySet
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.PAGE_SIZE
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.PinRequest
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
import one.mixin.android.session.Session
import one.mixin.android.session.encryptPin
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Account
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.PendingDeposit
import one.mixin.android.vo.PriceAndChange
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.TopAssetItem
import one.mixin.android.vo.User
import one.mixin.android.vo.toPriceAndChange
import one.mixin.android.vo.toSnapshot
import one.mixin.android.vo.toTopAssetItem
import javax.inject.Inject

@HiltViewModel
class WalletViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val assetRepository: AssetRepository,
    private val jobManager: MixinJobManager
) : ViewModel() {

    fun insertUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
        userRepository.upsert(user)
    }

    fun assetItems(): LiveData<List<AssetItem>> = assetRepository.assetItems()

    fun snapshotsFromDb(
        id: String,
        type: String? = null,
        otherType: String? = null,
        initialLoadKey: Int? = 0,
        orderByAmount: Boolean = false
    ): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(
            assetRepository.snapshotsFromDb(id, type, otherType, orderByAmount),
            PagedList.Config.Builder()
                .setPrefetchDistance(PAGE_SIZE)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build()
        )
            .setInitialLoadKey(initialLoadKey)
            .build()

    fun snapshotsByUserId(
        opponentId: String,
        initialLoadKey: Int? = 0
    ): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(
            assetRepository.snapshotsByUserId(opponentId),
            PagedList.Config.Builder()
                .setPrefetchDistance(PAGE_SIZE)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build()
        )
            .setInitialLoadKey(initialLoadKey)
            .build()

    suspend fun snapshotLocal(assetId: String, snapshotId: String) = assetRepository.snapshotLocal(assetId, snapshotId)

    fun assetItem(id: String): LiveData<AssetItem> = assetRepository.assetItem(id)

    suspend fun simpleAssetItem(id: String) = assetRepository.simpleAssetItem(id)

    fun updatePin(pin: String, oldPin: String?): Observable<MixinResponse<Account>> {
        val pinToken = Session.getPinToken()!!
        val old = encryptPin(pinToken, oldPin)
        val fresh = encryptPin(pinToken, pin)!!
        return accountRepository.updatePin(PinRequest(fresh, old)).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
    }

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

    fun hiddenAssets(): LiveData<List<AssetItem>> = assetRepository.hiddenAssetItems()

    fun addresses(id: String) = assetRepository.addresses(id)

    fun allSnapshots(type: String? = null, otherType: String? = null, initialLoadKey: Int? = 0, orderByAmount: Boolean = false):
        LiveData<PagedList<SnapshotItem>> =
            LivePagedListBuilder(
                assetRepository.allSnapshots(type, otherType, orderByAmount = orderByAmount),
                PagedList.Config.Builder()
                    .setPrefetchDistance(PAGE_SIZE * 2)
                    .setPageSize(PAGE_SIZE)
                    .setEnablePlaceholders(true)
                    .build()
            )
                .setInitialLoadKey(initialLoadKey)
                .build()

    suspend fun refreshPendingDeposits(asset: AssetItem) {
        handleMixinResponse(
            invokeNetwork = {
                assetRepository.pendingDeposits(asset.assetId, asset.destination, asset.tag)
            },
            successBlock = { list ->
                assetRepository.clearPendingDepositsByAssetId(asset.assetId)
                val pendingDeposits = list.data ?: return@handleMixinResponse

                pendingDeposits.chunked(100) { trunk ->
                    viewModelScope.launch(Dispatchers.IO) {
                        processPendingDepositTrunk(asset.assetId, trunk)
                    }
                }
            }
        )
    }

    private suspend fun processPendingDepositTrunk(assetId: String, trunk: List<PendingDeposit>) {
        val hashList = trunk.map { it.transactionHash }
        val existHashList = assetRepository.findSnapshotByTransactionHashList(assetId, hashList)
        trunk.filter {
            it.transactionHash !in existHashList
        }.map {
            it.toSnapshot(assetId)
        }.let {
            assetRepository.insertPendingDeposit(it)
        }
    }

    suspend fun getAsset(assetId: String) = withContext(Dispatchers.IO) {
        assetRepository.asset(assetId)
    }

    fun refreshHotAssets() {
        jobManager.addJobInBackground(RefreshTopAssetsJob())
    }

    fun refreshAsset(assetId: String? = null) {
        jobManager.addJobInBackground(RefreshAssetsJob(assetId))
    }

    suspend fun queryAsset(query: String): Pair<List<TopAssetItem>?, ArraySet<AssetItem>?> =
        withContext(Dispatchers.IO) {
            val response = try {
                assetRepository.queryAssets(query)
            } catch (t: Throwable) {
                ErrorHandler.handleError(t)
                return@withContext Pair(null, null)
            }
            if (response.isSuccess) {
                val assetList = response.data as List<Asset>
                val topAssetList = arrayListOf<TopAssetItem>()
                assetList.mapTo(topAssetList) { asset ->
                    var chainIconUrl = assetRepository.getIconUrl(asset.chainId)
                    if (chainIconUrl == null) {
                        chainIconUrl = fetchAsset(asset.chainId)
                    }
                    asset.toTopAssetItem(chainIconUrl)
                }
                val existsSet = ArraySet<AssetItem>()
                val needUpdatePrice = arrayListOf<PriceAndChange>()
                assetList.forEach {
                    val exists = assetRepository.findAssetItemById(it.assetId)
                    if (exists != null) {
                        needUpdatePrice.add(it.toPriceAndChange())
                        existsSet.add(exists)
                    }
                }
                if (needUpdatePrice.isNotEmpty()) {
                    assetRepository.suspendUpdatePrices(needUpdatePrice)
                }
                return@withContext Pair(topAssetList, existsSet)
            } else {
                ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
            }
            return@withContext Pair(null, null)
        }

    private suspend fun fetchAsset(assetId: String) = withContext(Dispatchers.IO) {
        val r = try {
            assetRepository.asset(assetId)
        } catch (t: Throwable) {
            ErrorHandler.handleError(t)
            return@withContext null
        }
        if (r.isSuccess) {
            r.data?.let {
                assetRepository.insert(it)
                return@withContext it.iconUrl
            }
        } else {
            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
        }
        return@withContext null
    }

    fun saveAssets(hotAssetList: List<TopAssetItem>) {
        hotAssetList.forEach {
            jobManager.addJobInBackground(RefreshAssetsJob(it.assetId))
        }
    }

    suspend fun findOrSyncAsset(assetId: String): AssetItem? {
        return withContext(Dispatchers.IO) {
            assetRepository.findOrSyncAsset(assetId)
        }
    }

    fun upsetAsset(asset: Asset) = viewModelScope.launch(Dispatchers.IO) {
        assetRepository.insert(asset)
    }

    fun observeTopAssets() = assetRepository.observeTopAssets()

    fun getUser(userId: String) = userRepository.getUserById(userId)

    suspend fun errorCount() = accountRepository.errorCount()

    fun refreshSnapshots(
        assetId: String? = null,
        offset: String? = null,
        opponent: String? = null
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
            }
        )

    suspend fun findAssetsByIds(ids: List<String>) = assetRepository.findAssetsByIds(ids)

    suspend fun fuzzySearchAssets(query: String?): List<AssetItem>? =
        if (query.isNullOrBlank()) {
            null
        } else {
            val escapedQuery = query.trim().escapeSql()
            assetRepository.fuzzySearchAssetIgnoreAmount(escapedQuery)
        }

    fun updateRecentSearchAssets(
        defaultSharedPreferences: SharedPreferences,
        assetId: String
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
                arr.joinToString("=")
            )
        } else {
            defaultSharedPreferences.putString(Constants.Account.PREF_RECENT_SEARCH_ASSETS, assetId)
        }
    }

    suspend fun ticker(assetId: String, offset: String?) = assetRepository.ticker(assetId, offset)

    suspend fun refreshSnapshot(snapshotId: String): SnapshotItem? {
        return withContext(Dispatchers.IO) {
            assetRepository.refreshAndGetSnapshot(snapshotId)
        }
    }

    suspend fun findSnapshot(snapshotId: String): SnapshotItem? =
        assetRepository.findSnapshotById(snapshotId)
}
