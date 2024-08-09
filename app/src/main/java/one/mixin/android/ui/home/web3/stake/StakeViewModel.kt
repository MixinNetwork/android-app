package one.mixin.android.ui.home.web3.stake

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.web3.StakeRequest
import one.mixin.android.api.response.web3.StakeAccountActivation
import one.mixin.android.api.response.web3.StakeResponse
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.ui.oldwallet.AssetRepository
import javax.inject.Inject

@HiltViewModel
class StakeViewModel
    @Inject
    internal constructor(
        private val assetRepository: AssetRepository,
    ) : ViewModel() {

    suspend fun stakeSol(stakeRequest: StakeRequest): StakeResponse? {
        return handleMixinResponse(
            invokeNetwork = { assetRepository.stakeSol(stakeRequest) },
            successBlock = {
               it.data
            }
        )
    }

    suspend fun getStakeAccountActivations(accounts: List<String>): List<StakeAccountActivation>? {
        return handleMixinResponse(
            invokeNetwork = { assetRepository.getStakeAccountActivations(accounts.joinToString(",")) },
            successBlock = {
                it.data
            }
        )
    }

    suspend fun getStakeValidators(votePubkeys: List<String>?): List<Validator>? {
        return handleMixinResponse(
            invokeNetwork = { assetRepository.getStakeValidators(votePubkeys?.joinToString(",")) },
            successBlock = {
                it.data
            }
        )
    }

    suspend fun searchStakeValidators(query: String): List<Validator>? {
        return handleMixinResponse(
            invokeNetwork = { assetRepository.searchStakeValidators(query) },
            successBlock = {
                it.data
            }
        )
    }
}
