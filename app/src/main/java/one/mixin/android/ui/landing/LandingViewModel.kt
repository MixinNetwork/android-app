package one.mixin.android.ui.landing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.DeactivateVerificationRequest
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.crypto.PinCipher
import one.mixin.android.repository.LandingAccountRepository
import one.mixin.android.tip.TipBody
import one.mixin.android.ui.landing.vo.MnemonicPhraseState
import one.mixin.android.vo.Account
import javax.inject.Inject

@HiltViewModel
class LandingViewModel
@Inject
internal constructor(
    private val landingAccountRepository: LandingAccountRepository,
    private val pinCipher: PinCipher,
) : ViewModel() {
    private val _mnemonicPhraseState = MutableLiveData<MnemonicPhraseState>(MnemonicPhraseState.Creating)
    val mnemonicPhraseState: LiveData<MnemonicPhraseState> get() = _mnemonicPhraseState

    fun loginVerification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        landingAccountRepository.verificationObserver(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun verification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        landingAccountRepository.verificationObserver(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    suspend fun create(
        id: String,
        request: AccountRequest,
    ): MixinResponse<Account> =
        withContext(Dispatchers.IO) {
            landingAccountRepository.create(id, request)
        }

    suspend fun changePhone(
        id: String,
        verificationCode: String,
        pin: String,
        saltBase64: String? = null,
    ): MixinResponse<Account> =
        landingAccountRepository.changePhone(
            id,
            AccountRequest(
                verificationCode,
                purpose = VerificationPurpose.PHONE.name,
                pin = pinCipher.encryptPin(pin, TipBody.forPhoneNumberUpdate(id, verificationCode)),
                saltBase64 = saltBase64,
            ),
        )

    fun deactiveVerification(
        id: String,
        code: String,
    ): Observable<MixinResponse<VerificationResponse>> =
        landingAccountRepository.deactiveVerification(
            id,
            DeactivateVerificationRequest(VerificationPurpose.DEACTIVATED.name, code),
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    suspend fun anonymousRequest(
        publicKeyHex: String,
        messageHex: String,
        signatureHex: String,
        hCaptchaResponse: String? = null,
        gRecaptchaResponse: String? = null,
        gtRecaptchaResponse: String? = null,
    ): MixinResponse<VerificationResponse> {
        val gt = gtRecaptchaResponse?.let { GTCaptcha4Utils.parseGTCaptchaResponse(it) }
        return landingAccountRepository.verification(
            VerificationRequest(
                purpose = VerificationPurpose.ANONYMOUS_SESSION.name,
                masterPublicHex = publicKeyHex,
                masterMessageHex = messageHex,
                masterSignatureHex = signatureHex,
                hCaptchaResponse = hCaptchaResponse,
                gRecaptchaResponse = gRecaptchaResponse,
                lotNumber = gt?.lotNumber,
                captchaOutput = gt?.captchaOutput,
                passToken = gt?.passToken,
                genTime = gt?.genTime,
            ),
        )
    }

    fun updateMnemonicPhraseState(state: MnemonicPhraseState) {
        _mnemonicPhraseState.value = state
    }
}
