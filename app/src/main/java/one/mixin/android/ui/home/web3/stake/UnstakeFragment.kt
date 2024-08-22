package one.mixin.android.ui.home.web3.stake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.api.request.web3.StakeAction
import one.mixin.android.api.request.web3.StakeRequest
import one.mixin.android.api.response.Web3ChainId
import one.mixin.android.api.response.solLamportToAmount
import one.mixin.android.api.response.web3.StakeAccount
import one.mixin.android.api.response.web3.StakeAccountActivation
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.api.response.web3.isActiveState
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navTo
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.TransactionStateFragment
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.js.SolanaTxSource

@AndroidEntryPoint
class UnstakeFragment : BaseFragment() {
    companion object {
        const val TAG = "UnstakeFragment"
        private const val ARGS_VALIDATOR = "args_validator"
        private const val ARGS_STAKE_ACCOUNT = "args_stake_account"
        private const val ARGS_STAKE_ACTIVATION = "args_stake_activation"

        fun newInstance(
            validator: Validator,
            stakeAccount: StakeAccount,
            stakeActivation: StakeAccountActivation,
        ) = UnstakeFragment().withArgs {
            putParcelable(ARGS_VALIDATOR, validator)
            putParcelable(ARGS_STAKE_ACCOUNT, stakeAccount)
            putParcelable(ARGS_STAKE_ACTIVATION, stakeActivation)
        }
    }

    private val stakeViewModel by viewModels<StakeViewModel>()

    private val validator by lazy { requireNotNull(requireArguments().getParcelableCompat(ARGS_VALIDATOR, Validator::class.java)) { "required validator cannot be null" } }
    private val stakeAccount by lazy { requireNotNull(requireArguments().getParcelableCompat(ARGS_STAKE_ACCOUNT, StakeAccount::class.java)) { "required stake_account cannot be null" } }
    private val stakeActivation by lazy { requireNotNull(requireArguments().getParcelableCompat(ARGS_STAKE_ACTIVATION, StakeAccountActivation::class.java)) { "required stake_activation cannot be null" } }

    private var isLoading by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(inflater.context).apply {
            setContent {
                UnstakePage(
                    validator,
                    stakeAccount,
                    stakeActivation,
                    isLoading,
                    onClick = { onClick() },
                ) {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }
    }

    private fun onClick() {
        lifecycleScope.launch {
            isLoading = true
            val stakeResp = stakeViewModel.stakeSol(StakeRequest(
                payer = JsSigner.solanaAddress,
                amount = stakeAccount.account.lamports.solLamportToAmount().toPlainString(),
                action = if (stakeActivation.state.isActiveState()) StakeAction.deactive.name else StakeAction.withdraw.name,
                pubkey = stakeAccount.pubkey,
            ))
            if (stakeResp == null) {
                isLoading = false
                return@launch
            }
            val signMessage = JsSignMessage(0, JsSignMessage.TYPE_RAW_TRANSACTION, Web3ChainId.SolanaChainId, data = stakeResp.tx, solanaTxSource = SolanaTxSource.InnerStake)
            JsSigner.useSolana()
            isLoading = false
            showBrowserBottomSheetDialogFragment(
                requireActivity(),
                signMessage,
                onTxhash = { _, serializedTx ->
                    lifecycleScope.launch {
                        activity?.onBackPressedDispatcher?.onBackPressed()
                        val txStateFragment =
                            TransactionStateFragment.newInstance(serializedTx, null).apply {
                                setCloseAction {
                                    parentFragmentManager.popBackStackImmediate()
                                    parentFragmentManager.popBackStackImmediate()
                                }
                            }
                        navTo(txStateFragment, TransactionStateFragment.TAG)
                    }
                },
            )
        }
    }
}