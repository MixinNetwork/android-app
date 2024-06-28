package one.mixin.android.ui.home.web3.stake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.api.request.web3.StakeAction
import one.mixin.android.api.request.web3.StakeRequest
import one.mixin.android.api.response.web3.StakeAccount
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navTo
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.TransactionStateFragment
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.js.SolanaTxSource
import java.math.BigDecimal

@AndroidEntryPoint
class StakeFragment : BaseFragment() {
    companion object {
        const val TAG = "StakeFragment"
        private const val ARGS_VALIDATOR = "args_validator"
        private const val ARGS_BALANCE = "args_balance"
        private const val ARGS_STAKE_ACCOUNT = "args_stake_account"

        fun newInstance(validator: Validator, balance: String, stakeAccount: StakeAccount?) = StakeFragment().withArgs {
            putParcelable(ARGS_VALIDATOR, validator)
            putString(ARGS_BALANCE, balance)
            stakeAccount?.let { putParcelable(ARGS_STAKE_ACCOUNT, it) }
        }
    }

    private val stakeViewModel by viewModels<StakeViewModel>()

    private val validator by lazy { requireNotNull(requireArguments().getParcelableCompat(ARGS_VALIDATOR, Validator::class.java)) { "required validator cannot be null" } }
    private val stakeAccount by lazy { requireArguments().getParcelableCompat(ARGS_STAKE_ACCOUNT, StakeAccount::class.java) }

    private var amountText: String by mutableStateOf("")
    private var isLoading by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val balance = requireNotNull(requireArguments().getString(ARGS_BALANCE))
        return ComposeView(inflater.context).apply {
            setContent {
                StakePage(
                    validator,
                    amountText,
                    balance,
                    isLoading,
                    onInputChanged = {
                        amountText = it
                    },
                    onChooseValidator = {
                        toast("click choose")
                    },
                    onMax = {
                        toast("click max")
                    },
                    onStake = { onStake() },
                ) {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }
    }

    private fun onStake() {
        lifecycleScope.launch {
            isLoading = true
            val amount =
                try {
                    BigDecimal(amountText).multiply(BigDecimal.TEN.pow(9)).toLong()
                } catch (e: Exception) {
                    return@launch
                }
            val stakeResp = stakeViewModel.stakeSol(StakeRequest(
                payer = JsSigner.solanaAddress,
                amount = amount,
                action = StakeAction.Delegate.name.lowercase(),
                vote = validator.votePubkey,
            ))
            if (stakeResp == null) {
                isLoading = false
                return@launch
            }
            val signMessage = JsSignMessage(0, JsSignMessage.TYPE_RAW_TRANSACTION, data = stakeResp.tx, solanaTxSource = SolanaTxSource.InnerSwap)
            JsSigner.useSolana()
            isLoading = false
            showBrowserBottomSheetDialogFragment(
                requireActivity(),
                signMessage,
                onTxhash = { _, serializedTx ->
                    lifecycleScope.launch {
                        val txStateFragment =
                            TransactionStateFragment.newInstance(serializedTx, null).apply {
                                setCloseAction {
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