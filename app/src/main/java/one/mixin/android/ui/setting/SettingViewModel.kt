package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.launch
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.DeauthorRequest
import one.mixin.android.api.service.AuthorizationService
import one.mixin.android.repository.AccountRepository
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.vo.App
import one.mixin.android.vo.UserRelationship
import javax.inject.Inject

class SettingViewModel @Inject
internal constructor(private val accountRepository: AccountRepository, private val authorizationService: AuthorizationService) : ViewModel() {

    fun countBlockingUsers() =
        accountRepository.findUsersByType(UserRelationship.BLOCKING.name)

    fun logout(): Observable<MixinResponse<Unit>> = accountRepository.logout()

    val apps: LiveData<List<App>> = accountRepository.apps()

    fun authorizations() = authorizationService.authorizations().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())!!

    @SuppressLint("CheckResult")
    fun deauthApp(appId: String) = accountRepository.deAuthorize(DeauthorRequest(appId)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())!!

    fun deleteApp(app: App) {
        launch(SINGLE_DB_THREAD) {
            accountRepository.deleteApp(app)
        }
    }
}