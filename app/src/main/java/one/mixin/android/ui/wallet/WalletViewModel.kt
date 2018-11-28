package one.mixin.android.ui.wallet

import android.util.ArraySet
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.work.WorkManager
import androidx.work.impl.WorkDatabase
import androidx.work.workDataOf
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.Constants
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.PinRequest
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAddressJob
import one.mixin.android.job.RefreshTopAssetsJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.UserRepository
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
import one.mixin.android.work.RefreshAssetsWorker
import javax.inject.Inject

class WalletViewModel @Inject
internal constructor(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val assetRepository: AssetRepository,
    private val jobManager: MixinJobManager
) : ViewModel() {

    fun redeem(code: String): Observable<MixinResponse<Account>> = accountRepository.redeem(code)
        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) {
        userRepository.upsert(user)
    }

    fun assetItems(): LiveData<List<AssetItem>> = assetRepository.assetItems()

    fun snapshotsFromDb(id: String, type: String? = null, otherType: String? = null): LiveData<List<SnapshotItem>> = assetRepository.snapshotsFromDb(id, type, otherType)

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

    fun updateAssetHidden(id: String, hidden: Boolean) = assetRepository.updateHidden(id, hidden)

    fun hiddenAssets(): LiveData<List<AssetItem>> = assetRepository.hiddenAssetItems()

    fun addresses(id: String) = assetRepository.addresses(id)

    fun allSnapshots(type: String? = null, otherType: String? = null, initialLoadKey: Int? = 0): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(assetRepository.allSnapshots(type, otherType), PagedList.Config.Builder()
            .setPrefetchDistance(Constants.PAGE_SIZE * 2)
            .setPageSize(Constants.PAGE_SIZE)
            .setEnablePlaceholders(true)
            .build())
            .setInitialLoadKey(initialLoadKey)
            .build()

    fun refreshAddressesByAssetId(assetId: String) {
        jobManager.addJobInBackground(RefreshAddressJob(assetId))
    }

    fun getAssetItem(assetId: String) = Flowable.just(assetId).map { assetRepository.simpleAssetItem(it) }
        .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())!!

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

    fun queryAsset(query: String): Pair<List<TopAssetItem>?, ArraySet<String>?> {
        val response = assetRepository.queryAssets(query).execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val assetList = response.data as List<Asset>
            val topAssetList = arrayListOf<TopAssetItem>()
            assetList.mapTo(topAssetList) { asset ->
                val chainIconUrl = assetRepository.getIconUrl(asset.chainId)
                asset.toTopAssetItem(chainIconUrl)
            }
            val existsSet = ArraySet<String>()
            topAssetList.forEach {
                val exists = assetRepository.checkExists(it.assetId)
                if (exists != null) {
                    existsSet.add(it.assetId)
                }
            }
            return Pair(topAssetList, existsSet)
        }
        return Pair(null, null)
    }

    fun saveAssets(hotAssetList: List<TopAssetItem>) {
        hotAssetList.forEach {
            WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshAssetsWorker>(
                workDataOf(RefreshAssetsWorker.ASSET_ID to it.assetId)
            )
        }
    }

    fun observeTopAssets() = assetRepository.observeTopAssets()

    fun getUser(userId: String) = userRepository.getUserById(userId)
}