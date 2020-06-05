package one.mixin.android.ui.url

import androidx.lifecycle.ViewModel
import one.mixin.android.repository.UserRepository
import javax.inject.Inject

class UrlInterpreterViewModel @Inject internal constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    suspend fun suspendFindUserById(userId: String) = userRepository.suspendFindUserById(userId)

    suspend fun findAppById(id: String) = userRepository.findAppById(id)
}
