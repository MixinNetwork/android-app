package one.mixin.android.ui.home.bot

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import one.mixin.android.repository.UserRepository

class BotManagerViewModel @ViewModelInject internal constructor(val userRepository: UserRepository) : ViewModel() {

    suspend fun getNotTopApps(appIds: List<String>) = userRepository.getNotTopApps(appIds)

    suspend fun findAppById(appId: String) = userRepository.findAppById(appId)

    suspend fun findUserByAppId(appId: String) = userRepository.findUserByAppId(appId)
}
