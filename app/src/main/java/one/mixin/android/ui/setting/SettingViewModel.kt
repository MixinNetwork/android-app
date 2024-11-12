package one.mixin.android.ui.setting

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.ContactRequest
import one.mixin.android.api.request.DeactivateRequest
import one.mixin.android.api.request.DeauthorRequest
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.api.service.AuthorizationService
import one.mixin.android.api.service.ContactService
import one.mixin.android.crypto.PinCipher
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.tip.TipBody
import one.mixin.android.vo.LogResponse
import one.mixin.android.vo.UserRelationship
import javax.inject.Inject

@HiltViewModel
class SettingViewModel
    @Inject
    internal constructor(
        private val accountRepository: AccountRepository,
        private val authorizationService: AuthorizationService,
        private val userRepository: UserRepository,
        private val contactService: ContactService,
        private val tokenRepository: TokenRepository,
        private val pinCipher: PinCipher,
    ) : ViewModel() {
        suspend fun verification(request: VerificationRequest): MixinResponse<VerificationResponse> =
            accountRepository.verification(request)

        fun countBlockingUsers() =
            accountRepository.findUsersByType(UserRelationship.BLOCKING.name)

        fun authorizations() =
            authorizationService.authorizations().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())!!

        fun deauthApp(appId: String) =
            accountRepository.deAuthorize(DeauthorRequest(appId)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())!!

        suspend fun findUserById(userId: String) = userRepository.suspendFindUserById(userId)

        suspend fun getContacts() = contactService.contacts()

        suspend fun deleteContacts() =
            withContext(Dispatchers.IO) {
                contactService.syncContacts(emptyList())
            }

        suspend fun syncContacts(contactRequests: List<ContactRequest>) =
            withContext(Dispatchers.IO) {
                contactService.syncContacts(contactRequests)
            }

        suspend fun getPinLogs(offset: String? = null): MixinResponse<List<LogResponse>> {
            return withContext(Dispatchers.IO) {
                accountRepository.getPinLogs(offset = offset)
            }
        }

        suspend fun preferences(request: AccountUpdateRequest) =
            withContext(Dispatchers.IO) {
                accountRepository.preferences(request)
            }

        suspend fun simpleAssetsWithBalance() = tokenRepository.simpleAssetsWithBalance()

        suspend fun refreshUser(userId: String) = userRepository.refreshUser(userId)

        suspend fun findAllAssetIdSuspend() = tokenRepository.findAllAssetIdSuspend()

        suspend fun getDeactivateTipBody(
            userId: String,
            pin: String,
        ): String = pinCipher.encryptPin(pin, TipBody.forDeactivate(userId))

        suspend fun deactivate(request: DeactivateRequest) = accountRepository.deactivate(request)
}
