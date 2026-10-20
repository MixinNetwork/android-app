package one.mixin.android.ui.setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.api.service.UserService
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import javax.inject.Inject

@HiltViewModel
class SettingBlockedViewModel
    @Inject
    internal constructor(
        private val userService: UserService,
        private val accountRepository: AccountRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        fun blockingUsers(): LiveData<List<User>> {
            viewModelScope.launch {
                val response = runCatching { userService.blockingUsers() }.getOrNull() ?: return@launch
                if (!response.isSuccess) return@launch
                response.data?.forEach { user ->
                    user.relationship = UserRelationship.BLOCKING.name
                    userRepository.upsertBlock(user)
                }
            }
            return accountRepository.findUsersByType(UserRelationship.BLOCKING.name)
        }
    }
