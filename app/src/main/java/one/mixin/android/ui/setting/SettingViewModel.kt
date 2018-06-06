package one.mixin.android.ui.setting

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import one.mixin.android.api.MixinResponse
import one.mixin.android.repository.AccountRepository
import one.mixin.android.vo.UserRelationship
import javax.inject.Inject

class SettingViewModel @Inject
internal constructor(private val accountRepository: AccountRepository) : ViewModel() {

    fun countBlockingUsers() =
        accountRepository.findUsersByType(UserRelationship.BLOCKING.name)

    fun logout(): Observable<MixinResponse<Unit>> = accountRepository.logout()
}