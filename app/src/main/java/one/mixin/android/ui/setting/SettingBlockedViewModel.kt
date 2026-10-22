package one.mixin.android.ui.setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
        suspend fun refreshBlockingUsers() {
            val response = try {
                userService.blockingUsers()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return
            }
            if (!response.isSuccess) return
            response.data?.forEach { user ->
                user.relationship = UserRelationship.BLOCKING.name
                userRepository.upsertBlock(user)
            }
        }

        fun blockingUsers(): LiveData<List<User>> = accountRepository.findUsersByType(UserRelationship.BLOCKING.name)
    }
