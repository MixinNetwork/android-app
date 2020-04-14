package one.mixin.android.ui.home.bot

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import one.mixin.android.repository.UserRepository

class BotManagerViewModel @Inject internal constructor(val userRepository: UserRepository) : ViewModel() {
    suspend fun getApps() =
        userRepository.getApps()

    suspend fun getTopApps(appIds: List<String>) = userRepository.getTopApps(appIds)
}
