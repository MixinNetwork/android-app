package one.mixin.android.ui.landing.viewmodel

import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.DeactivateVerificationRequest
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.api.request.SessionSecretRequest
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.ExportRequest
import one.mixin.android.api.response.UserSession
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.EmergencyService
import one.mixin.android.api.service.SignalKeyService
import one.mixin.android.api.service.UserService
import one.mixin.android.crypto.PinCipher
import one.mixin.android.crypto.db.SenderKeyDao
import one.mixin.android.crypto.db.SessionDao
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.crypto.vo.SenderKey
import one.mixin.android.crypto.vo.Session
import one.mixin.android.db.DatabaseProvider
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.getDeviceId
import one.mixin.android.job.RefreshOneTimePreKeysJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.tip.TipBody
import one.mixin.android.ui.landing.vo.MnemonicPhraseState
import one.mixin.android.ui.landing.vo.SetupState
import one.mixin.android.vo.Account
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.User
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class LandingViewModel
@Inject
internal constructor(
    private val databaseProvider: DatabaseProvider,
    private val accountService: AccountService,
    private val emergencyService: EmergencyService,
    private val pinCipher: PinCipher,
) : ViewModel() {

    fun lockAndUpgradeDatabase() = databaseProvider.getMixinDatabase().runInTransaction {
        // do nothing
    }

    fun pingServer(
        callback: () -> Unit,
        elseCallBack: (e: Exception?) -> Unit,
    ): Job {
        return viewModelScope.launch {
            try {
                val response =
                    withContext(coroutineContext + Dispatchers.IO) {
                        accountService.ping().execute()
                    }
                response.headers()["X-Server-Time"]?.toLong()?.let { serverTime ->
                    if (abs(serverTime / 1000000 - System.currentTimeMillis()) < Constants.ALLOW_INTERVAL) { // 10 minutes
                        callback.invoke()
                    } else {
                        elseCallBack.invoke(null)
                    }
                }
            } catch (e: Exception) {
                elseCallBack.invoke(e)
            }
        }
    }

    fun insertUser(user: User) =
        viewModelScope.launch(Dispatchers.IO) {
            databaseProvider.getMixinDatabase().userDao().insertSuspend(user)
        }

    fun loginVerification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        accountService.verificationObserver(request).subscribeOn(Schedulers.io()).observeOn(
            AndroidSchedulers.mainThread())

    fun verification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        accountService.verificationObserver(request).subscribeOn(Schedulers.io()).observeOn(
            AndroidSchedulers.mainThread())

    suspend fun create(
        id: String,
        request: AccountRequest,
    ): MixinResponse<Account> =
        withContext(Dispatchers.IO) {
            accountService.create(id, request)
        }

    suspend fun changePhone(
        id: String,
        verificationCode: String,
        pin: String,
        saltBase64: String? = null,
    ): MixinResponse<Account> =
        accountService.changePhone(
            id,
            AccountRequest(
                verificationCode,
                purpose = VerificationPurpose.PHONE.name,
                pin = pinCipher.encryptPin(pin, TipBody.forPhoneNumberUpdate(id, verificationCode)),
                saltBase64 = saltBase64
            ),
        )

    fun deactiveVerification(
        id: String,
        code: String,
    ): Observable<MixinResponse<VerificationResponse>> =
        accountService.deactiveVerification(
            id,
            DeactivateVerificationRequest(VerificationPurpose.DEACTIVATED.name, code),
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
        accountService.update(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun updatePhone(
        id: String,
        phone: String,
    ) = {
        databaseProvider.getMixinDatabase().userDao().updatePhone(id, phone)
    }

    private val _mnemonicPhraseState =
        MutableLiveData<MnemonicPhraseState>(MnemonicPhraseState.Initial)
    val mnemonicPhraseState: LiveData<MnemonicPhraseState> get() = _mnemonicPhraseState

    fun updateMnemonicPhraseState(state: MnemonicPhraseState){
        _mnemonicPhraseState.value = state
    }

    suspend fun anonymousRequest(publicKeyHex: String, messageHex: String, signatureHex: String, hCaptchaResponse: String? = null, gRecaptchaResponse: String? = null): MixinResponse<VerificationResponse> {
        val r = accountService.verification(
            VerificationRequest(
                purpose = VerificationPurpose.ANONYMOUS_SESSION.name,
                masterPublicHex = publicKeyHex,
                masterMessageHex = messageHex,
                masterSignatureHex = signatureHex,
                hCaptchaResponse = hCaptchaResponse,
                gRecaptchaResponse = gRecaptchaResponse
            )
        )
        return r
    }

    private val _setupState = MutableLiveData<SetupState>(SetupState.Loading)
    val setupState: LiveData<SetupState> get() = _setupState

    fun setState(state: SetupState): SetupState? {
        _setupState.value = state
        return _setupState.value
    }

    suspend fun getEncryptedTipBody(
        userId: String,
        pin: String,
    ): String = pinCipher.encryptPin(pin, TipBody.forExport(userId))

    suspend fun saltExport(exportRequest: ExportRequest) = accountService.saltExport(exportRequest)

    suspend fun createVerifyEmergency(
        id: String,
        request: EmergencyRequest,
    ) = emergencyService.createVerify(id, request)

    suspend fun loginVerifyEmergency(
        id: String,
        request: EmergencyRequest,
    ) = emergencyService.loginVerify(id, request)
}