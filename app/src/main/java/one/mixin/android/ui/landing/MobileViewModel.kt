package one.mixin.android.ui.landing

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.util.Session
import one.mixin.android.util.encryptPin
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import javax.inject.Inject

class MobileViewModel @Inject internal
constructor(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    fun verification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        accountRepository.verification(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun create(id: String, request: AccountRequest): Observable<MixinResponse<Account>> =
        accountRepository.create(id, request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun changePhone(id: String, verificationCode: String, pin: String): Observable<MixinResponse<Account>> =
        Observable.just(Session.getPinToken()).map { pinToken ->
            accountRepository.changePhone(id, AccountRequest(verificationCode, purpose = VerificationPurpose.PHONE.name,
                pin = encryptPin(pinToken, pin))).execute().body()!!
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
        accountRepository.update(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) {
        userRepository.upsert(user)
    }

    fun updatePhone(id: String, phone: String) = userRepository.updatePhone(id, phone)
}