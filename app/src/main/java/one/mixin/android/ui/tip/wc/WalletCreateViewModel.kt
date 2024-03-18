package one.mixin.android.ui.tip.wc

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import one.mixin.android.repository.TokenRepository
import one.mixin.android.tip.Tip
import one.mixin.android.tip.tipPrivToAddress
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import javax.inject.Inject

@HiltViewModel
class WalletCreateViewModel
@Inject
internal constructor(
    private val tip: Tip,
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

    suspend fun getTipAddress(
        context: Context,
        pin: String,
        chainId:String
    ): String {
        val result = tip.getOrRecoverTipPriv(context, pin)
        val spendKey = tip.getSpendPrivFromEncryptedSalt(tip.getEncryptedSalt(context), pin, result.getOrThrow())
        val privateKey = tipPrivToPrivateKey(spendKey)
        return tipPrivToAddress(privateKey, chainId)
    }
}
