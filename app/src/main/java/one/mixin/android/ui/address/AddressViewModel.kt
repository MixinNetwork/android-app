package one.mixin.android.ui.address

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAddressJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.TokenRepository
import javax.inject.Inject

@HiltViewModel
class AddressViewModel
@Inject
internal constructor(
    private val tokenRepository: TokenRepository,
    private val accountRepository: AccountRepository,
    private val jobManager: MixinJobManager,
) : ViewModel() {
    fun addressesFlow(chainId: String) = tokenRepository.addressesFlow(chainId)

    suspend fun validateExternalAddress(
        assetId: String,
        destination: String,
        tag: String?
    ) =
        accountRepository.validateExternalAddress(assetId, destination, tag)
}
