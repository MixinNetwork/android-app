package one.mixin.android.web3.details

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.response.Web3Transaction
import one.mixin.android.databinding.FragmentWeb3TransactionBinding
import one.mixin.android.event.TokenEvent
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.util.viewBinding
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
        ) = Web3TransactionFragment().withArgs {
            putParcelable(ARGS_TRANSACTION, transaction)
            putString(ARGS_CHAIN, chain)
        }
    }

    private val binding by viewBinding(FragmentWeb3TransactionBinding::bind)
    private val web3ViewModel by viewModels<Web3ViewModel>()

    private val transaction by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_TRANSACTION, Web3Transaction::class.java))
    }

    private val chain by lazy {
        requireNotNull(requireArguments().getString(ARGS_CHAIN))
    }

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
                transaction.event?.let {
                    lifecycleScope.launch {
                        web3ViewModel.web3Token(
                            chain, it.chainId, it.address
                        ) ?: return@launch
                        RxBus.publish(it)
                    }
                }
            }
            val badge = transaction.badge
            if (badge == null) {
                avatar.badge.isVisible = false
            } else {
                avatar.badge.isVisible = true
                avatar.badge.loadImage(badge, R.drawable.ic_avatar_place_holder)
            }
            feeTv.text = "${transaction.fee.amount.numberFormat8()} ${transaction.fee.symbol}"
            dateTv.text = transaction.createdAt.fullDate()
            statusTv.text = transaction.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }
}
