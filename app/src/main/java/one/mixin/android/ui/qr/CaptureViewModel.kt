package one.mixin.android.ui.qr

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.AppExecutors
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.response.PaymentResponse
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.Asset
import one.mixin.android.vo.User
import org.jetbrains.anko.doAsync
import javax.inject.Inject

class CaptureViewModel
@Inject
internal constructor(
    private val accountRepository: AccountRepository,
    private val assetRepository: AssetRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    fun searchCode(code: String) = accountRepository.searchCode(code)

    fun pay(request: TransferRequest): Observable<MixinResponse<PaymentResponse>> =
        assetRepository.pay(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun queryUser(userId: String): User? {
        return userRepository.getUserById(userId)
    }

    fun saveAsset(asset: Asset) {
        AppExecutors().diskIO().execute {
            assetRepository.upsert(asset)
        }
    }

    fun saveUser(user: User) {
        doAsync {
            userRepository.upsert(user)
        }
    }
}