package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.databinding.FragmentWaitingBottomSheetBinding
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class Web3WaitingBottomSheetDialogFragment() : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "Web3WaitingBottomSheetDialogFragment"

        fun newInstance(chainId: String) = Web3WaitingBottomSheetDialogFragment().withArgs {
            putString("chain", chainId)
        }
    }

    private val binding by viewBinding(FragmentWaitingBottomSheetBinding::inflate)

    private val web3ViewModel by viewModels<Web3ViewModel>()


    @Inject
    lateinit var jobManager: MixinJobManager

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }

        binding.apply {
            continueTv.setOnClickListener {
                dismiss()
            }
        }
        lifecycleScope.launch {
            val chainId = arguments?.getString("chain")
            refreshTransactionData(chainId)
        }
    }

    private suspend fun refreshTransactionData(chainId: String?) {
        chainId ?: return
        try {
            while (true) {
                val pendingRawTransaction = web3ViewModel.getPendingTransactions(chainId)
                if (pendingRawTransaction.isEmpty()) {
                    dismiss()
                } else {
                    pendingRawTransaction.forEach { transition ->
                        val r = web3ViewModel.transaction(transition.hash, transition.chainId)
                        if (r.isSuccess && (r.data?.state ==  TransactionType.TxSuccess.value || r.data?.state == TransactionType.TxFailed.value)) {
                            web3ViewModel.deletePending(transition.hash, transition.chainId)
                            web3ViewModel.insertRawTranscation(r.data!!)
                        }
                    }
                    delay(5_000)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
