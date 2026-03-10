package one.mixin.android.ui.landing

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.User
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class MobileViewModel
@Inject
internal constructor(
    private val accountRepositoryProvider: Provider<AccountRepository>,
    private val userRepositoryProvider: Provider<UserRepository>,
    private val jobManager: MixinJobManager,
) : ViewModel() {
    private val accountRepository: AccountRepository
        get() = accountRepositoryProvider.get()

    private val userRepository: UserRepository
        get() = userRepositoryProvider.get()

    fun insertUser(user: User) =
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.insertUser(user)
        }

    fun update(request: AccountUpdateRequest) = accountRepository.update(request)
}
