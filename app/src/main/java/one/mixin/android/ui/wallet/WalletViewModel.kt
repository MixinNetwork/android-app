package one.mixin.android.ui.wallet

import androidx.collection.ArraySet
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.PinRequest
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshTopAssetsJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.wallet.BaseTransactionsFragment.Companion.PAGE_SIZE
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.util.encryptPin
import one.mixin.android.vo.Account
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.TopAssetItem
import one.mixin.android.vo.User
import one.mixin.android.vo.toTopAssetItem
import javax.inject.Inject

class WalletViewModel @Inject
internal constructor(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val assetRepository: AssetRepository,
    private val jobManager: MixinJobManager
) : ViewModel() {

    fun insertUser(user: User) {
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
        LivePagedListBuilder(assetRepository.snapshotsFromDb(id, type, otherType, orderByAmount), PagedList.Config.Builder()
            .setPrefetchDistance(PAGE_SIZE)
            .setPageSize(PAGE_SIZE)
            .setEnablePlaceholders(true)
            .build())
            .setInitialLoadKey(initialLoadKey)
            .build()

    fun snapshotsByUserId(opponentId: String): LiveData<List<SnapshotItem>> = assetRepository.snapshotsByUserId(opponentId)

    fun snapshotLocal(assetId: String, snapshotId: String) = assetRepository.snapshotLocal(assetId, snapshotId)

    fun assetItem(id: String): LiveData<AssetItem> = assetRepository.assetItem(id)

    fun simpleAssetItem(id: String) = assetRepository.simpleAssetItem(id)

    fun updatePin(pin: String, oldPin: String?): Observable<MixinResponse<Account>> {
        val pinToken = Session.getPinToken()!!
        val old = encryptPin(pinToken, oldPin)
        val fresh = encryptPin(pinToken, pin)!!
        return accountRepository.updatePin(PinRequest(fresh, old)).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
    }

    fun verifyPin(code: String) = accountRepository.verifyPin(code)

    fun getUserById(id: String): User? = userRepository.getUserById(id)

    fun checkAndRefreshUsers(userIds: List<String>) = runBlocking {
        viewModelScope.launch {
            val existUsers = userRepository.findUserExist(userIds)
            val queryUsers = userIds.filter {
                !existUsers.contains(it)
            }
            if (queryUsers.isEmpty()) {
                return@launch
            }
            jobManager.addJobInBackground(RefreshUserJob(queryUsers))
        }
    }

    fun updateAssetHidden(id: String, hidden: Boolean) = assetRepository.updateHidden(id, hidden)

    fun hiddenAssets(): LiveData<List<AssetItem>> = assetRepository.hiddenAssetItems()

    fun addresses(id: String) = assetRepository.addresses(id)

    fun allSnapshots(type: String? = null, otherType: String? = null, initialLoadKey: Int? = 0, orderByAmount: Boolean = false):
        LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(assetRepository.allSnapshots(type, otherType, orderByAmount = orderByAmount), PagedList.Config.Builder()
            .setPrefetchDistance(PAGE_SIZE * 2)
            .setPageSize(PAGE_SIZE)
            .setEnablePlaceholders(true)
            .build())
            .setInitialLoadKey(initialLoadKey)
            .build()

    fun getAssetItem(assetId: String) = Flowable.just(assetId).map { assetRepository.simpleAssetItem(it) }
        .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())

    fun pendingDeposits(asset: String, key: String? = null, name: String? = null, tag: String? = null) = assetRepository.pendingDeposits(asset, key, name, tag)
        .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())!!

    fun insertPendingDeposit(snapshot: List<Snapshot>) = assetRepository.insertPendingDeposit(snapshot)

    fun clearPendingDepositsByAssetId(assetId: String) = assetRepository.clearPendingDepositsByAssetId(assetId)

    fun getAsset(assetId: String): Flowable<MixinResponse<Asset>?> = Flowable.just(assetId).map {
        assetRepository.asset(assetId).execute().body()
    }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())

    fun refreshHotAssets() {
        jobManager.addJobInBackground(RefreshTopAssetsJob())
    }

    suspend fun queryAsset(query: String): Pair<List<TopAssetItem>?, ArraySet<String>?> =
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
                val existsSet = ArraySet<String>()
                topAssetList.forEach {
                    val exists = assetRepository.checkExists(it.assetId)
                    if (exists != null) {
                        existsSet.add(it.assetId)
                    }
                }
                return@withContext Pair(topAssetList, existsSet)
            } else {
                ErrorHandler.handleMixinError(response.errorCode)
            }
            return@withContext Pair(null, null)
        }

    private suspend fun fetchAsset(assetId: String) = withContext(Dispatchers.IO) {
        val r = try {
            assetRepository.suspendAsset(assetId)
        } catch (t: Throwable) {
            ErrorHandler.handleError(t)
            return@withContext null
        }
        if (r.isSuccess) {
            r.data?.let {
                assetRepository.upsert(it)
                return@withContext it.iconUrl
            }
        } else {
            ErrorHandler.handleMixinError(r.errorCode)
        }
        return@withContext null
    }

    fun saveAssets(hotAssetList: List<TopAssetItem>) {
        hotAssetList.forEach {
            jobManager.addJobInBackground(RefreshAssetsJob(it.assetId))
        }
    }

    fun observeTopAssets() = assetRepository.observeTopAssets()

    fun getUser(userId: String) = userRepository.getUserById(userId)
}