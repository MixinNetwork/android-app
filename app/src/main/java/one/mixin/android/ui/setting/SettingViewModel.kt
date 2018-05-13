package one.mixin.android.ui.setting

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import one.mixin.android.api.MixinResponse
import one.mixin.android.repository.AccountRepository
import javax.inject.Inject

class SettingViewModel @Inject
internal constructor(private val accountRepository: AccountRepository) : ViewModel() {

    fun logout(): Observable<MixinResponse<Unit>> = accountRepository.logout()
}