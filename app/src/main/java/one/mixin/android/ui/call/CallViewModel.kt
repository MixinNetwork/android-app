package one.mixin.android.ui.call

import androidx.lifecycle.ViewModel
import one.mixin.android.repository.UserRepository
import javax.inject.Inject

class CallViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    suspend fun findMultiUsersByIds(ids: Set<String>) = userRepository.findMultiUsersByIds(ids)
}
