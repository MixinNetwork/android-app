package one.mixin.android.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.api.service.EmergencyService
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.User
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class EmergencyViewModel
    @Inject
    internal constructor(
        private val emergencyService: EmergencyService,
        private val accountRepositoryProvider: Provider<AccountRepository>,
        private val userRepositoryProvider: Provider<UserRepository>,
    ) : ViewModel() {
        suspend fun createEmergency(request: EmergencyRequest) =
            withContext(Dispatchers.IO) {
                emergencyService.create(request)
            }

        suspend fun createVerifyEmergency(
            id: String,
            request: EmergencyRequest,
        ) =
            withContext(Dispatchers.IO) {
                emergencyService.createVerify(id, request)
            }

        suspend fun loginVerifyEmergency(
            id: String,
            request: EmergencyRequest,
        ) =
            withContext(Dispatchers.IO) {
                emergencyService.loginVerify(id, request)
            }

        suspend fun findFriendsNotBot() = userRepositoryProvider.get().findFriendsNotBot()

        suspend fun findUserById(userId: String) = userRepositoryProvider.get().suspendFindUserById(userId)

        suspend fun showEmergency(pin: String) =
            withContext(Dispatchers.IO) {
                accountRepositoryProvider.get().showEmergency(pin)
            }

        fun insertUser(u: User) =
            viewModelScope.launch(Dispatchers.IO) {
                accountRepositoryProvider.get().insertUserSuspend(u)
            }

        suspend fun deleteEmergency(pin: String) =
            withContext(Dispatchers.IO) {
                accountRepositoryProvider.get().deleteEmergency(pin)
            }
    }
