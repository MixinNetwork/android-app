package one.mixin.android.ui.home.web3.swap

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.web3.SwapRequest
import one.mixin.android.api.response.web3.QuoteResponse
import one.mixin.android.api.response.web3.SwapResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.repository.TokenRepository
import one.mixin.android.ui.oldwallet.AssetRepository
import javax.inject.Inject

@HiltViewModel
class SwapViewModel
@Inject
internal constructor(
    private val tokenRepository: TokenRepository,
    private val assetRepository: AssetRepository,
) : ViewModel() {

    suspend fun web3Tokens(): MixinResponse<List<SwapToken>> = assetRepository.web3Tokens()

    suspend fun web3Quote(
        inputMint: String,
        outputMint: String,
        amount: String,
    ): MixinResponse<QuoteResponse> = assetRepository.web3Quote(inputMint, outputMint, amount)

    suspend fun web3Swap(
        swapRequest: SwapRequest,
    ): MixinResponse<SwapResponse> = assetRepository.web3Swap(swapRequest)
}