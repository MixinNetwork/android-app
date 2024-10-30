package one.mixin.android.ui.landing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.DeactivateVerificationRequest
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.crypto.PinCipher
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.tip.TipBody
import one.mixin.android.ui.landing.vo.MnemonicPhraseState
import one.mixin.android.ui.landing.vo.SetupState
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import javax.inject.Inject

@HiltViewModel
class MobileViewModel
@Inject
internal constructor(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val jobManager: MixinJobManager,
    private val pinCipher: PinCipher,
) : ViewModel() {
    fun loginVerification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        accountRepository.verificationObserver(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun verification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        accountRepository.verificationObserver(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun upsertUser(u: User) =
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.upsert(u)
        }

    suspend fun create(
        id: String,
        request: AccountRequest,
    ): MixinResponse<Account> =
        withContext(Dispatchers.IO) {
            accountRepository.create(id, request)
        }

    suspend fun changePhone(
        id: String,
        verificationCode: String,
        pin: String,
        saltBase64: String? = null,
    ): MixinResponse<Account> =
        accountRepository.changePhone(
            id,
            AccountRequest(
                verificationCode,
                purpose = VerificationPurpose.PHONE.name,
                pin = pinCipher.encryptPin(pin, TipBody.forPhoneNumberUpdate(id, verificationCode)),
                salt_base64 = saltBase64
            ),
        )

    fun deactiveVerification(
        id: String,
        code: String,
    ): Observable<MixinResponse<VerificationResponse>> =
        accountRepository.deactiveVerification(
            id,
            DeactivateVerificationRequest(VerificationPurpose.DEACTIVATED.name, code),
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
        accountRepository.update(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) =
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.upsert(user)
        }

    fun updatePhone(
        id: String,
        phone: String,
    ) = userRepository.updatePhone(id, phone)

    private val _mnemonicPhraseState = MutableLiveData<MnemonicPhraseState>(MnemonicPhraseState.Initial)
    val mnemonicPhraseState: LiveData<MnemonicPhraseState> get() = _mnemonicPhraseState

    fun updateMnemonicPhraseState(state: MnemonicPhraseState){
        _mnemonicPhraseState.value = state
    }

    suspend fun anonymousRequest(publicKeyHex: String, messageHex: String, signatureHex: String, hCaptchaResponse: String? = null, gRecaptchaResponse: String? = null): MixinResponse<VerificationResponse> {
        val r = accountRepository.verification(
            VerificationRequest(
                purpose = VerificationPurpose.ANONYMOUS_SESSION.name,
                public_key_hex = publicKeyHex,
                message_hex = messageHex,
                signature_hex = signatureHex,
                hCaptchaResponse = hCaptchaResponse,
                gRecaptchaResponse = gRecaptchaResponse
            )
        )
        return r
    }

    private val _setupState = MutableLiveData<SetupState>(SetupState.Loading)
    val setupState: LiveData<SetupState> get() = _setupState

    suspend fun mockLoadingToFailure(): SetupState? {
        delay(2000)
        _setupState.value = SetupState.Failure
        return _setupState.value
    }

    suspend fun mockLoadingToSuccess(): SetupState? {
        delay(1000)
        _setupState.value = SetupState.Success
        return _setupState.value
    }
}
