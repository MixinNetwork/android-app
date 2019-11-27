package one.mixin.android.ui.common.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.repository.AccountRepository
import javax.inject.Inject

class MySharedAppsViewModel
@Inject internal constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {
    suspend fun addFavoriteApp(appId: String) = accountRepository.addFavoriteApp(appId)

    suspend fun getUserFavoriteApps(userId: String) = accountRepository.getUserFavoriteApps(userId)

    suspend fun removeFavoriteApp(appId: String) = accountRepository.removeFavoriteApp(appId)

    suspend fun getApps() = withContext(Dispatchers.IO) {
        accountRepository.getApps()
    }
}