package one.mixin.android.web3.details

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWeb3TransactionBinding
import one.mixin.android.databinding.ViewWalletWeb3TransactionBottomBinding
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.details.Web3TransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class Web3TransactionFragment : BaseFragment(R.layout.fragment_web3_transaction) {
    companion object {
        const val TAG = "Web3TransactionFragment"
        const val ARGS_TRANSACTION = "args_transaction"
        const val ARGS_CHAIN = "args_chain"

        fun newInstance(
            transaction: Web3TransactionItem,
            chain: String,
            web3Token: Web3TokenItem,
        ) = Web3TransactionFragment().withArgs {
            putParcelable(ARGS_TRANSACTION, transaction)
            putString(ARGS_CHAIN, chain)
            putParcelable(ARGS_TOKEN, web3Token)
        }
    }

    private val binding by viewBinding(FragmentWeb3TransactionBinding::bind)
    private val web3ViewModel by viewModels<Web3ViewModel>()
    private val token: Web3TokenItem by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, Web3TokenItem::class.java)!!
    }

    private val transaction by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_TRANSACTION, Web3TransactionItem::class.java))
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
            showBottom()
        }
        binding.root.isClickable = true
        binding.apply {
            transactionHashTv.text = transaction.transactionHash
            val amountColor =
                if (transaction.status == TransactionStatus.FAILED.value || transaction.status == TransactionStatus.NOT_FOUND.value) {
                    requireContext().colorFromAttribute(R.attr.text_assist)
                } else if (transaction.transactionType == TransactionType.TRANSFER_OUT.value) {
                    requireContext().getColor(R.color.wallet_pink)
                } else if (transaction.transactionType == TransactionType.TRANSFER_IN.value) {
                    requireContext().getColor(R.color.wallet_green)
                } else {
                    requireContext().colorFromAttribute(R.attr.text_primary)
                }
            val symbolColor = requireContext().colorFromAttribute(R.attr.text_primary)
            
            val mainAmount = transaction.getFormattedAmount()
            
            valueTv.text = buildAmountSymbol(requireContext(),
                when (transaction.transactionType) {
                    TransactionType.TRANSFER_OUT.value -> "-$mainAmount"
                    TransactionType.TRANSFER_IN.value -> "+$mainAmount"
                    else -> mainAmount
                }, transaction.chainSymbol ?: "", amountColor, symbolColor
            )
            
            when(transaction.status) {
                TransactionStatus.SUCCESS.value -> {
                    status.text = getString(R.string.Completed)
                    status.setTextColor(requireContext().getColor(R.color.wallet_green))
                    status.setBackgroundResource(R.drawable.bg_status_success)
                }
                TransactionStatus.PENDING.value -> {
                    status.text = getString(R.string.Pending)
                    status.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
                    status.setBackgroundResource(R.drawable.bg_status_default)
                }
                TransactionStatus.FAILED.value -> {
                    status.text = getString(R.string.Failed)
                    status.setTextColor(requireContext().getColor(R.color.wallet_pink))
                    status.setBackgroundResource(R.drawable.bg_status_failed)
                }
                else -> {
                    status.text = getString(R.string.Not_found)
                    status.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
                    status.setBackgroundResource(R.drawable.bg_status_default)
                }
            }
            
            val fromAddress = if (transaction.senders.isNotEmpty()) transaction.senders[0].from ?: "" else ""
            val toAddress = if (transaction.receivers.isNotEmpty()) transaction.receivers[0].to ?: "" else ""
            
            fromTv.text = fromAddress
            toTv.text = toAddress
            
            avatar.bg.loadImage(transaction.chainIconUrl, R.drawable.ic_avatar_place_holder)
            avatar.setOnClickListener {
                tokenClick(transaction)
            }
            
            avatar.badge.isVisible = false
            
            dateTv.text = transaction.transactionAt.fullDate()
            feeLl.isVisible = true
            feeTv.text = "${transaction.fee} ${transaction.chainSymbol ?: ""}"
            statusLl.isVisible = false
        }
    }

    private fun tokenClick(transaction: Web3TransactionItem) {
        // 处理点击事件
    }

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomBinding = ViewWalletWeb3TransactionBottomBinding.bind(
            View.inflate(
                ContextThemeWrapper(
                    requireActivity(),
                    R.style.Custom
                ), R.layout.view_wallet_web3_transaction_bottom, null
            )
        )
        builder.setCustomView(bottomBinding.root)
        val bottomSheet = builder.create()
        bottomBinding.apply {
            explorer.setOnClickListener {
                val url = "${Constants.API.URL}/external/explore/${token.chainId}/transactions/${transaction.transactionHash}"
                context?.openUrl(url)
                bottomSheet.dismiss()
            }
            cancel.setOnClickListener { bottomSheet.dismiss() }
        }

        bottomSheet.show()
    }
}
