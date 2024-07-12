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
import one.mixin.android.api.response.web3.Validator
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navTo
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.TransactionStateFragment
import one.mixin.android.ui.home.web3.showBrowserBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.swap.SwapFragment.Companion.maxLeftAmount
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import one.mixin.android.web3.js.SolanaTxSource
import java.math.BigDecimal
import java.math.RoundingMode

@AndroidEntryPoint
class StakeFragment : BaseFragment() {
    companion object {
        const val TAG = "StakeFragment"
        private const val ARGS_VALIDATOR = "args_validator"
        const val ARGS_BALANCE = "args_balance"

        fun newInstance(validator: Validator, balance: String) = StakeFragment().withArgs {
            putParcelable(ARGS_VALIDATOR, validator)
            putString(ARGS_BALANCE, balance)
        }
    }

    private val stakeViewModel by viewModels<StakeViewModel>()

    private var amountText: String by mutableStateOf("")
    private var isLoading by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        var validator: Validator by mutableStateOf(requireNotNull(requireArguments().getParcelableCompat(ARGS_VALIDATOR, Validator::class.java)) { "required validator cannot be null" })
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
                        navTo(ValidatorsFragment.newInstance().apply {
                            setOnSelect { v ->
                                validator = v
                            }
                        }, ValidatorsFragment.TAG)
                    },
                    onMax = {
                        amountText = calcMax(balance)
                    },
                    onStake = { onStake(validator) },
                ) {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }
    }

    private fun onStake(validator: Validator) {
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
                action = StakeAction.delegate.name.lowercase(),
                vote = validator.votePubkey,
            ))
            if (stakeResp == null) {
                isLoading = false
                return@launch
            }
            val signMessage = JsSignMessage(0, JsSignMessage.TYPE_RAW_TRANSACTION, data = stakeResp.tx, solanaTxSource = SolanaTxSource.InnerStake)
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
                                    parentFragmentManager.popBackStackImmediate()
                                }
                            }
                        navTo(txStateFragment, TransactionStateFragment.TAG)
                    }
                },
            )
        }
    }

    private fun calcMax(balance: String): String {
        val calc = fun(balance: BigDecimal): String {
            return balance.setScale(9, RoundingMode.CEILING).stripTrailingZeros().toPlainString()
        }
        var b = BigDecimal(balance)
        if (b <= BigDecimal(maxLeftAmount)) {
            return calc(b)
        }
        b = b.subtract(BigDecimal(maxLeftAmount))
        return calc(b)
    }
}