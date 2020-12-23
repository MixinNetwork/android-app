package one.mixin.android.ui.preview

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import one.mixin.android.repository.UserRepository

class TextPreviewViewModel
@ViewModelInject
internal constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    suspend fun findUserByIdentityNumberSuspend(identityNumber: String) =
        userRepository.findUserByIdentityNumberSuspend(identityNumber)
}
