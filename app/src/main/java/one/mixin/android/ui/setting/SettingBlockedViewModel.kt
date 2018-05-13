package one.mixin.android.ui.setting

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.service.UserService
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import javax.inject.Inject

class SettingBlockedViewModel @Inject
internal constructor(
    private val userService: UserService,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository
)
    : ViewModel() {

    fun blockingUsers(scopeProvider: AndroidLifecycleScopeProvider): LiveData<List<User>> {
        userService.blockingUsers().subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
            .autoDisposable(scopeProvider).subscribe({
                if (it.isSuccess) {
                    it.data?.let {
                        for (user in it) {
                            user.relationship = UserRelationship.BLOCKING.name
                            userRepository.upsertBlock(user)
                        }
                    }
                }
            }, {})
        return accountRepository.findUsersByType(UserRelationship.BLOCKING.name)
    }
}