package one.mixin.android.ui.wallet.key

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import one.mixin.android.repository.TokenRepository
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import javax.inject.Inject

@HiltViewModel
class KeyViewModel
@Inject
internal constructor(
    val tokenRepository: TokenRepository,
) : ViewModel() {
    private val _status = MutableStateFlow(TransferStatus.AWAITING_CONFIRMATION)
    val status = _status.asStateFlow()
    var errorMessage: String? = null
    var key: String? = null

    fun updateStatus(status: TransferStatus) {
        _status.value = status
    }

    fun fail(error:String) {
        _status.value = TransferStatus.FAILED
        this.errorMessage = error
    }

    fun success(key:String) {
        _status.value = TransferStatus.SUCCESSFUL
        this.key = key
    }
}
