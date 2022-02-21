package one.mixin.android.ui.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.DeactivateVerificationRequest
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncFts4Job
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.session.encryptPin
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import javax.inject.Inject

@HiltViewModel
class MobileViewModel @Inject internal
constructor(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val jobManager: MixinJobManager
) : ViewModel() {

    fun loginVerification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        accountRepository.verificationObserver(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun verification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        accountRepository.verificationObserver(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    suspend fun create(id: String, request: AccountRequest): MixinResponse<Account> = withContext(Dispatchers.IO) {
        accountRepository.create(id, request)
    }

    fun changePhone(id: String, verificationCode: String, pin: String): Observable<MixinResponse<Account>> =
        accountRepository.changePhone(
            id,
            AccountRequest(
                verificationCode,
                purpose = VerificationPurpose.PHONE.name,
                pin = encryptPin(Session.getPinToken()!!, pin)
            )
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun deactiveVerification(id: String, code: String): Observable<MixinResponse<VerificationResponse>> =
        accountRepository.deactiveVerification(
            id, DeactivateVerificationRequest(VerificationPurpose.DEACTIVATED.name, code)
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    suspend fun deactivate(pin: String, verificationId: String, code: String) = accountRepository.deactivate(pin, verificationId)

    fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
        accountRepository.update(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
        userRepository.upsert(user)
    }

    fun updatePhone(id: String, phone: String) = userRepository.updatePhone(id, phone)

    fun startSyncFts4Job() {
        jobManager.addJobInBackground(SyncFts4Job())
    }
}
