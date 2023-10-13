package one.mixin.android.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.User
import javax.inject.Inject

@HiltViewModel
class EmergencyViewModel
@Inject
internal constructor(
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    suspend fun createEmergency(request: EmergencyRequest) = withContext(Dispatchers.IO) {
        accountRepository.createEmergency(request)
    }

    suspend fun createVerifyEmergency(id: String, request: EmergencyRequest) = withContext(Dispatchers.IO) {
        accountRepository.createVerifyEmergency(id, request)
    }

    suspend fun loginVerifyEmergency(id: String, request: EmergencyRequest) = withContext(Dispatchers.IO) {
        accountRepository.loginVerifyEmergency(id, request)
    }

    suspend fun findFriendsNotBot() = userRepository.findFriendsNotBot()

    suspend fun findUserById(userId: String) = userRepository.suspendFindUserById(userId)

    suspend fun showEmergency(pin: String) = withContext(Dispatchers.IO) {
        accountRepository.showEmergency(pin)
    }

    fun upsertUser(u: User) = viewModelScope.launch(Dispatchers.IO) {
        userRepository.upsert(u)
    }

    suspend fun deleteEmergency(pin: String) = withContext(Dispatchers.IO) {
        accountRepository.deleteEmergency(pin)
    }
}
