package one.mixin.android.ui.tip.wc

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.api.DataErrorException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ServerErrorException
import one.mixin.android.databinding.FragmentWalletUnlockBottomSheetBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.visibleDisplayHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.exception.TipNodeException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.data.TransferStatus
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import org.chromium.net.CronetException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException

@AndroidEntryPoint
class WalletUnlockBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "WalletUnlockBottomSheetDialogFragment"
        const val ARGS_TYPE = "type"
        const val TYPE_ETH = "eth"
        const val TYPE_POLYGON = "polygon"
        const val TYPE_BSC = "bsc"

        fun newInstance(type:String) =
            WalletUnlockBottomSheetDialogFragment().withArgs {
                putString(ARGS_TYPE, type)
            }
    }

    private val type by lazy {
        requireArguments().getString(ARGS_TYPE, TYPE_ETH)
    }

    private val keyViewModel by viewModels<WalletUnlockViewModel>()

    private val binding by viewBinding(FragmentWalletUnlockBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi", "SetTextI18n", "StringFormatMatches")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
            setCustomViewHeight(requireActivity().visibleDisplayHeight())
        }
        val chain = when (type) {
            TYPE_POLYGON -> getString(R.string.Polygon)
            TYPE_BSC -> getString(R.string.BSC)
            else -> getString(R.string.Ethereum)
        }
        val otherChain = when (type) {
            TYPE_POLYGON -> arrayOf(getString(R.string.Polygon), getString(R.string.Ethereum), getString(R.string.BSC))
            TYPE_BSC -> arrayOf(getString(R.string.BSC), getString(R.string.Ethereum), getString(R.string.Polygon))
            else -> arrayOf(getString(R.string.Ethereum), getString(R.string.Polygon), getString(R.string.BSC))
        }
        binding.apply {
            agreement1.text = getString(R.string.unlock_web3_account_agreement_1, chain)
            agreement2.text = getString(R.string.unlock_web3_account_agreement_2, chain)
            agreement3.text = getString(R.string.unlock_web3_account_agreement_3, *otherChain)
            bottom.setOnClickListener({
                dismiss()
            }, {
                createAccount()
            }, {
                dismiss()
            })
        }

        lifecycleScope.launch {
            keyViewModel.status.collect { status ->
                when (status) {
                    TransferStatus.FAILED -> {
                        binding.contentVa.displayedChild = 0
                        binding.header.filed(keyViewModel.errorMessage)
                        binding.bottom.updateStatus(TransferStatus.FAILED)
                    }

                    TransferStatus.SUCCESSFUL -> {
                        binding.header.success()
                        binding.contentVa.displayedChild = 1
                        keyViewModel.address?.let { key ->
                            binding.key.text = key
                        }
                        binding.bottom.updateStatus(TransferStatus.SUCCESSFUL)
                    }

                    TransferStatus.IN_PROGRESS -> {
                        binding.contentVa.displayedChild = 0
                        binding.header.progress()
                        binding.bottom.updateStatus(TransferStatus.IN_PROGRESS)
                    }

                    else -> {
                        binding.contentVa.displayedChild = 0
                        binding.header.awaiting(getString(R.string.unlock_web3_account, chain), getString(R.string.unlock_web3_account_description, chain))
                    }
                }
            }
        }
    }

    private fun handleError(throwable: Throwable) {
        keyViewModel.errorMessage =
            when (throwable) {
                is IOException ->
                    when (throwable) {
                        is SocketTimeoutException -> getString(R.string.error_connection_timeout)
                        is UnknownHostException -> getString(R.string.No_network_connection)
                        is NetworkException -> getString(R.string.No_network_connection)
                        is DataErrorException -> getString(R.string.Data_error)
                        is CronetException -> {
                            val extra =
                                if (throwable is org.chromium.net.NetworkException) {
                                    val e = throwable
                                    "${e.errorCode}, ${e.cronetInternalErrorCode}"
                                } else {
                                    ""
                                }
                            "${getString(R.string.error_connection_error)} $extra"
                        }
                        is ServerErrorException -> getString(R.string.error_server_5xx_code, throwable.code)

                        else -> getString(R.string.error_unknown_with_message, throwable.message)
                    }

                is TipNodeException -> {
                    throwable.getTipExceptionMsg(requireContext())
                }

                is ExecutionException -> {
                    if (throwable.cause is CronetException) {
                        val extra =
                            if (throwable is org.chromium.net.NetworkException) {
                                val e = throwable as org.chromium.net.NetworkException
                                "${e.errorCode}, ${e.cronetInternalErrorCode}"
                            } else {
                                ""
                            }
                        "${getString(R.string.error_connection_error)} $extra"
                    } else {
                        getString(R.string.error_connection_error)
                    }
                }

                else -> getString(R.string.error_unknown_with_message, throwable.message)
            }
    }

    private fun createAccount() {
        keyViewModel.updateStatus(TransferStatus.IN_PROGRESS)
        PinInputBottomSheetDialogFragment.newInstance(biometricInfo = null, from = 1).setOnPinComplete { pin ->
            lifecycleScope.launch(
                CoroutineExceptionHandler { _, error ->
                    handleError(error)
                    keyViewModel.updateStatus(TransferStatus.FAILED)
                },
            ) {
                val address = keyViewModel.getTipAddress(requireContext(), pin, ETHEREUM_CHAIN_ID)
                PropertyHelper.updateKeyValue(Constants.Account.PREF_WALLET_CONNECT_ADDRESS, address)
                keyViewModel.success(address)
            }
        }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }
}
