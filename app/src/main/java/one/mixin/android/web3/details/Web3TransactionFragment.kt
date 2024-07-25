package one.mixin.android.web3.details

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.Web3Transaction
import one.mixin.android.databinding.FragmentWeb3TransactionBinding
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.details.Web3TransactionDetailsFragment.Companion.ARGS_TOKEN
import timber.log.Timber
import java.util.Locale

@AndroidEntryPoint
class Web3TransactionFragment : BaseFragment(R.layout.fragment_web3_transaction) {
    companion object {
        const val TAG = "Web3TransactionFragment"
        const val ARGS_TRANSACTION = "args_transaction"
        const val ARGS_CHAIN = "args_chain"

        fun newInstance(
            transaction: Web3Transaction,
            chain: String,
            web3Token: Web3Token,
        ) = Web3TransactionFragment().withArgs {
            putParcelable(ARGS_TRANSACTION, transaction)
            putString(ARGS_CHAIN, chain)
            putParcelable(ARGS_TOKEN, web3Token)
        }
    }

    private val binding by viewBinding(FragmentWeb3TransactionBinding::bind)
    private val web3ViewModel by viewModels<Web3ViewModel>()
    private val token: Web3Token by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, Web3Token::class.java)!!
    }

    private val transaction by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_TRANSACTION, Web3Transaction::class.java))
    }

    private val chain by lazy {
        requireNotNull(requireArguments().getString(ARGS_CHAIN))
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        binding.titleView.rightAnimator.visibility = View.VISIBLE
        binding.titleView.rightIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.root.isClickable = true
        binding.apply {
            transactionIdTv.text = transaction.id
            transactionHashTv.text = transaction.transactionHash
            valueTv.text = transaction.value(requireContext())
            valueAsTv.text = transaction.valueAs
            fromTv.text = transaction.sender
            toTv.text = transaction.receiver
            avatar.bg.loadImage(transaction.icon, R.drawable.ic_avatar_place_holder)
            avatar.setOnClickListener {
                tokenClick(transaction)
            }
            val badge = transaction.badge
            if (badge == null) {
                avatar.badge.isVisible = false
            } else {
                avatar.badge.isVisible = true
                avatar.badge.loadImage(badge, R.drawable.ic_avatar_place_holder)
            }
            feeTv.text = "${transaction.fee.amount} ${transaction.fee.symbol}"
            dateTv.text = transaction.createdAt.fullDate()
            statusTv.text = transaction.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    private fun tokenClick(transaction: Web3Transaction) {
        lifecycleScope.launch {
            transaction.event?.let { event ->
                if (event.address == token.assetKey) {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    return@launch
                }
                val dialog =
                    indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                        setCancelable(false)
                    }
                try {
                    web3ViewModel.web3Token(
                        chain, event.chainId, event.address
                    ) ?: return@launch
                    dialog.dismiss()
                    RxBus.publish(event)
                } catch (e: Exception) {
                    Timber.e(e)
                    dialog.dismiss()
                    return@launch
                }
            }
        }
    }
}
