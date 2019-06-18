package one.mixin.android.ui.setting

import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.request.DeauthorRequest
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.api.service.AuthorizationService
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.UserRelationship
import javax.inject.Inject

class SettingViewModel @Inject
internal constructor(
    private val accountRepository: AccountRepository,
    private val authorizationService: AuthorizationService,
    private val userRepository: UserRepository
) : ViewModel() {

    fun countBlockingUsers() =
        accountRepository.findUsersByType(UserRelationship.BLOCKING.name)

    fun authorizations() = authorizationService.authorizations().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())!!

    fun deauthApp(appId: String) = accountRepository.deAuthorize(DeauthorRequest(appId)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())!!

    suspend fun showEmergency(pin: String) = accountRepository.showEmergency(pin)

    suspend fun findUserById(userId: String) = userRepository.suspendFindUserById(userId)
}