package one.mixin.android.ui.preview

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class TextPreviewViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository,
) : ViewModel() {
    suspend fun findUserByIdentityNumberSuspend(identityNumber: String) =
        userRepository.findUserByIdentityNumberSuspend(identityNumber)
}
