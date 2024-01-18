package one.mixin.android.ui.common.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.repository.AccountRepository
import one.mixin.android.util.ErrorHandler
import javax.inject.Inject

@HiltViewModel
class MySharedAppsViewModel
    @Inject
    internal constructor(
        private val accountRepository: AccountRepository,
    ) : ViewModel() {
        suspend fun addFavoriteApp(appId: String) =
            withContext(Dispatchers.IO) {
                val response = accountRepository.addFavoriteApp(appId)
                return@withContext if (response.isSuccess) {
                    accountRepository.insertFavoriteApp(response.data!!)
                    true
                } else {
                    ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
                    false
                }
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

        suspend fun getFavoriteAppsByUserId(userId: String) =
            withContext(Dispatchers.IO) {
                accountRepository.getFavoriteAppsByUserId(userId)
            }

        suspend fun getUnfavoriteApps() =
            withContext(Dispatchers.IO) {
                accountRepository.getUnfavoriteApps()
            }

        suspend fun removeFavoriteApp(
            appId: String,
            userId: String,
        ) =
            withContext(Dispatchers.IO) {
                val response = accountRepository.removeFavoriteApp(appId)
                return@withContext if (response.isSuccess) {
                    accountRepository.deleteByAppIdAndUserId(appId, userId)
                    true
                } else {
                    ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
                    false
                }
            }

        suspend fun getApps() =
            withContext(Dispatchers.IO) {
                accountRepository.getApps()
            }

        suspend fun getAllApps() = withContext(Dispatchers.IO) {
            accountRepository.getAllApps()
        }
    }
