package one.mixin.android.ui.tip.wc

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import one.mixin.android.repository.TokenRepository
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import javax.inject.Inject

@HiltViewModel
class WalletCreateViewModel
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
        this.errorMessage = error
        _status.value = TransferStatus.FAILED
    }

    fun success(key:String) {
        this.key = key
        _status.value = TransferStatus.SUCCESSFUL
    }
}
