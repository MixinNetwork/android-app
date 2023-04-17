package one.mixin.android.ui.landing

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.DeactivateVerificationRequest
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.EmergencyService
import one.mixin.android.crypto.PinCipher
import one.mixin.android.tip.TipBody
import one.mixin.android.vo.Account
import javax.inject.Inject

@HiltViewModel
class MobileViewModel @Inject internal
constructor(
    private val accountService: AccountService,
    private val emergencyService: EmergencyService,
    private val pinCipher: PinCipher,
) : ViewModel() {

    fun loginVerification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        accountService.verificationObserver(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun verification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        accountService.verificationObserver(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    suspend fun create(id: String, request: AccountRequest): MixinResponse<Account> = withContext(Dispatchers.IO) {
        accountService.create(id, request)
    }

    suspend fun changePhone(id: String, verificationCode: String, pin: String): MixinResponse<Account> =
        accountService.changePhone(
            id,
            AccountRequest(
                verificationCode,
                purpose = VerificationPurpose.PHONE.name,
                pin = pinCipher.encryptPin(pin, TipBody.forPhoneNumberUpdate(id, verificationCode)),
            ),
        )

    suspend fun createEmergency(request: EmergencyRequest) = withContext(Dispatchers.IO) {
        emergencyService.create(request)
    }

    suspend fun createVerifyEmergency(id: String, request: EmergencyRequest) = withContext(Dispatchers.IO) {
        emergencyService.createVerify(id, request)
    }

    suspend fun loginVerifyEmergency(id: String, request: EmergencyRequest) = withContext(Dispatchers.IO) {
        emergencyService.loginVerify(id, request)
    }

    fun deactiveVerification(id: String, code: String): Observable<MixinResponse<VerificationResponse>> =
        accountService.deactiveVerification(
            id,
            DeactivateVerificationRequest(VerificationPurpose.DEACTIVATED.name, code),
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
        accountService.update(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}
