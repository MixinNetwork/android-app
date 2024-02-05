package one.mixin.android.ui.home.bot

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class BotManagerViewModel
    @Inject
    internal constructor(val userRepository: UserRepository, val accountRepository: AccountRepository) : ViewModel() {
        suspend fun findAppById(appId: String) = userRepository.findAppById(appId)

        suspend fun findUserByAppId(appId: String) = userRepository.findUserByAppId(appId)

        suspend fun refreshUser(userId: String) = userRepository.refreshUser(userId)

        suspend fun getFavoriteAppsByUserId(userId: String) =
            withContext(Dispatchers.IO) {
                accountRepository.getFavoriteAppsByUserId(userId)
            }

        suspend fun getAllExploreApps() =
            withContext(Dispatchers.IO) {
                accountRepository.getAllExploreApps()
            }

        suspend fun refreshFavoriteApps(userId: String) =
            withContext(Dispatchers.IO) {
                val response = accountRepository.getUserFavoriteApps(userId)
                if (response.isSuccess) {
                    response.data?.let { list ->
                        accountRepository.insertFavoriteApps(userId, list)
                    }
                }
            }
    }
