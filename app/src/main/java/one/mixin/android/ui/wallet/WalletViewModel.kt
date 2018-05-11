package one.mixin.android.ui.wallet

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AssetFee
import one.mixin.android.api.request.PinRequest
import one.mixin.android.crypto.aesEncrypt
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.Account
import one.mixin.android.vo.Address
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.User
import javax.inject.Inject

class WalletViewModel @Inject
internal constructor(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val assetRepository: AssetRepository
) : ViewModel() {

    fun redeem(code: String): Observable<MixinResponse<Account>> = accountRepository.redeem(code)
        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) {
        userRepository.upsert(user)
    }

    fun assetItems(): LiveData<List<AssetItem>> = assetRepository.assetItems()

    fun snapshotsFromDb(id: String): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(assetRepository.snapshotsFromDb(id), PagedList.Config.Builder()
            .setPageSize(20).build()).build()

    fun snapshotLocal(assetId: String, snapshotId: String) = assetRepository.snapshotLocal(assetId, snapshotId)

    fun assetItem(id: String): LiveData<AssetItem> = assetRepository.assetItem(id)

    fun simpleAssetItem(id: String) = assetRepository.simpleAssetItem(id)

    fun updatePin(pin: String, oldPin: String?): Observable<MixinResponse<Account>> =
        accountRepository.getPinToken().map { pinToken ->
            val old = aesEncrypt(pinToken, oldPin)
            val fresh = aesEncrypt(pinToken, pin)!!
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

    fun getFeeAndRefreshAddresses(assetId: String): Observable<MixinResponse<AssetFee>> {
        val o1 = assetRepository.assetsFee(assetId)
        val o2 = assetRepository.refreshAddresses(assetId)
        return Observable.zip(o1, o2, BiFunction<MixinResponse<AssetFee>, MixinResponse<List<Address>>,
            MixinResponse<AssetFee>> { t1, t2 ->
            t2.data?.let {
                assetRepository.insertAddresses(it)
            }
            return@BiFunction t1
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    fun allSnapshots(): LiveData<PagedList<SnapshotItem>> =
        LivePagedListBuilder(assetRepository.allSnapshots(), PagedList.Config.Builder()
            .setPageSize(10).build()).build()
}