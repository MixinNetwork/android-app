package one.mixin.android.ui.setting

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.repository.TokenRepository
import javax.inject.Inject

@HiltViewModel
class LogAndDebugViewModel @Inject constructor(
    private val tokenRepository: TokenRepository
) : ViewModel() {

    suspend fun deleteAllWeb3Transactions() {
        tokenRepository.deleteAllWeb3Transactions()
    }
}
