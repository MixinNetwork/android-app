package one.mixin.android.ui.wallet

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.PinRequest
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAddressJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.util.encryptPin
import one.mixin.android.vo.Account
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.User
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

    fun snapshotsFromDb(id: String): LiveData<List<SnapshotItem>> = assetRepository.snapshotsFromDb(id)

    fun snapshotLocal(assetId: String, snapshotId: String) = assetRepository.snapshotLocal(assetId, snapshotId)

    fun assetItem(id: String): LiveData<AssetItem> = assetRepository.assetItem(id)

    fun simpleAssetItem(id: String) = assetRepository.simpleAssetItem(id)

    fun updatePin(pin: String, oldPin: String?): Observable<MixinResponse<Account>> =
        accountRepository.getPinToken().map { pinToken ->
            val old = encryptPin(pinToken, oldPin)
            val fresh = encryptPin(pinToken, pin)!!
            accountRepository.updatePin(PinRequest(fresh, old)).execute().body()!!
        }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())

    fun verifyPin(code: String) = accountRepository.verifyPin(code)

    fun getUserById(id: String): User? = userRepository.getUserById(id)

    fun updateAssetHidden(id: String, hidden: Boolean) = assetRepository.updateHidden(id, hidden)

    fun hiddenAssets(): LiveData<List<AssetItem>> = assetRepository.hiddenAssetItems()

    fun addresses(id: String) = assetRepository.addresses(id)

    fun checkAsset(id: String): Asset? {
        val a = assetRepository.assetLocal(id)
        if (a != null) return a

        val response = assetRepository.asset(id).execute().body()
        if (response != null && response.isSuccess) {
            response.data?.let {
                assetRepository.insertAsset(it)
                return it
            }
        }
        return null
    }

    fun allSnapshots(): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(assetRepository.allSnapshots(), PagedList.Config.Builder()
            .setPageSize(10).build()).build()

    fun refreshAddressesByAssetId(assetId: String) {
        jobManager.addJobInBackground(RefreshAddressJob(assetId))
    }
}