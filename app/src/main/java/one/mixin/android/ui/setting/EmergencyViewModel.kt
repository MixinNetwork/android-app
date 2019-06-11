package one.mixin.android.ui.setting

import androidx.lifecycle.ViewModel
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import javax.inject.Inject

class EmergencyViewModel @Inject
internal constructor(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    suspend fun createEmergency(request: EmergencyRequest) =
        accountRepository.createEmergency(request)

    suspend fun verifyEmergency(id: String, request: EmergencyRequest) =
        accountRepository.verifyEmergency(id, request)

    suspend fun showEmergency() = accountRepository.showEmergency()

    suspend fun getFriendsNotBot() = userRepository.getFriendsNotBot()

    suspend fun findUserById(userId: String) = userRepository.suspendFindUserById(userId)
}