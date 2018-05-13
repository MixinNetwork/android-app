package one.mixin.android.ui.setting

import android.arch.lifecycle.ViewModel
import one.mixin.android.repository.AccountRepository
import one.mixin.android.vo.UserRelationship
import javax.inject.Inject

class SettingPrivacyViewModel @Inject
internal constructor(private val accountRepository: AccountRepository) : ViewModel() {
    fun countBlockingUsers() =
        accountRepository.findUsersByType(UserRelationship.BLOCKING.name)
}