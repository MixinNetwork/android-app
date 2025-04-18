package one.mixin.android.web3.details

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
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
        requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_TRANSACTION,
                Web3TransactionItem::class.java
            )
        )
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
        binding.titleView.rightExtraIb.visibility = View.VISIBLE
        binding.titleView.rightExtraIb.setImageResource(R.drawable.ic_support)
        binding.titleView.rightExtraIb.setOnClickListener {
            context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
        }
        binding.root.isClickable = true
        binding.apply {
            transactionHashTv.text = transaction.transactionHash
            val amountColor = if (transaction.status == TransactionStatus.PENDING.value || transaction.status == TransactionStatus.NOT_FOUND.value || transaction.status == TransactionStatus.FAILED.value) {
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

            valueTv.text = when (transaction.transactionType) {
                TransactionType.SWAP.value -> {
                    valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    valueTv.setTypeface(valueTv.typeface, Typeface.BOLD)
                    valueTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
                    getString(R.string.Swap)
                }
                TransactionType.UNKNOWN.value -> {
                    valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    valueTv.setTypeface(valueTv.typeface, Typeface.BOLD)
                    valueTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
                    getString(R.string.Unknown)
                }
                TransactionType.APPROVAL.value -> {
                    valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    valueTv.setTypeface(valueTv.typeface, Typeface.BOLD)
                    valueTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_primary))
                    getString(R.string.Approval)
                }
                else -> {
                    buildAmountSymbol(
                        requireContext(),
                        when (transaction.transactionType) {
                            TransactionType.TRANSFER_OUT.value -> "-$mainAmount"
                            TransactionType.APPROVAL.value -> "-$mainAmount"
                            TransactionType.TRANSFER_IN.value -> "+$mainAmount"
                            else -> mainAmount
                        },
                        when (transaction.transactionType) {
                            TransactionType.TRANSFER_OUT.value -> transaction.sendAssetSymbol ?: ""
                            TransactionType.APPROVAL.value -> transaction.sendAssetSymbol ?: ""
                            TransactionType.TRANSFER_IN.value -> transaction.receiveAssetSymbol ?: ""
                            else -> ""
                        },
                        amountColor, symbolColor
                    )
                }
            }

            when (transaction.status) {
                TransactionStatus.SUCCESS.value -> {
                    status.text = getString(R.string.Completed)
                    status.setTextColor(requireContext().getColor(R.color.wallet_green))
                    status.setBackgroundResource(R.drawable.bg_status_success)
                }

                TransactionStatus.PENDING.value -> {
                    status.text = getString(R.string.Pending)
                    status.setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
                    status.setBackgroundResource(R.drawable.bg_status_default)
                }

                TransactionStatus.FAILED.value -> {
                    status.text = getString(R.string.Failed)
                    status.setTextColor(requireContext().getColor(R.color.wallet_pink))
                    status.setBackgroundResource(R.drawable.bg_status_failed)
                }

                TransactionStatus.NOT_FOUND.value -> {
                    status.text = getString(R.string.Expired)
                    status.setTextColor(requireContext().getColor(R.color.wallet_pink))
                    status.setBackgroundResource(R.drawable.bg_status_failed)
                }

                else -> {
                    status.text = transaction.status
                    status.setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
                    status.setBackgroundResource(R.drawable.bg_status_default)
                }
            }

            val fromAddress = transaction.getFromAddress()
            val toAddress = transaction.getToAddress()
            
            when  {
                transaction.status == TransactionStatus.NOT_FOUND.value || transaction.status == TransactionStatus.FAILED.value -> {
                    valueTv.isVisible = false
                    fromLl.isVisible = false
                    toLl.isVisible = false
                }
                transaction.transactionType == TransactionType.TRANSFER_IN.value -> {
                    fromTv.text = fromAddress
                    fromLl.isVisible = true
                    toLl.isVisible = false
                }
                transaction.transactionType == TransactionType.TRANSFER_OUT.value -> {
                    toTv.text = toAddress
                    fromLl.isVisible = false
                    toLl.isVisible = true
                }
                transaction.transactionType ==TransactionType.APPROVAL.value -> {
                    toTv.text = toAddress
                    fromLl.isVisible = false
                    toLl.isVisible = true
                }
                transaction.transactionType ==TransactionType.UNKNOWN.value -> {
                    valueTv.isVisible = false
                    fromLl.isVisible = false
                    toLl.isVisible = false
                }
                else -> {
                    fromLl.isVisible = false
                    toLl.isVisible = false
                }
            }

            when {
                transaction.status == TransactionStatus.NOT_FOUND.value || transaction.status == TransactionStatus.FAILED.value || transaction.status == TransactionStatus.PENDING.value -> {
                    avatar.bg.setImageResource(R.drawable.ic_web3_transaction_contract)
                }

                transaction.transactionType == TransactionType.TRANSFER_OUT.value -> {
                    avatar.bg.loadImage(transaction.sendAssetIconUrl, R.drawable.ic_avatar_place_holder)
                }

                transaction.transactionType == TransactionType.TRANSFER_IN.value -> {
                    avatar.bg.loadImage(transaction.receiveAssetIconUrl, R.drawable.ic_avatar_place_holder)
                }

                transaction.transactionType == TransactionType.SWAP.value -> {
                    avatar.bg.setImageResource(R.drawable.ic_web3_transaction_swap)
                }

                transaction.transactionType == TransactionType.APPROVAL.value -> {
                    avatar.bg.setImageResource(R.drawable.ic_web3_transaction_approval)
                }

                else -> {
                    avatar.bg.setImageResource(R.drawable.ic_web3_transaction_contract)
                }
            }

            avatar.setOnClickListener {
                tokenClick(transaction)
            }

            avatar.badge.isVisible = false

            dateTv.text = transaction.transactionAt.fullDate()
            feeLl.isVisible = transaction.fee.isNotEmpty()
            feeTv.text = "${transaction.fee} ${transaction.chainSymbol ?: ""}"
            statusLl.isVisible = false
            
            networkLl.isVisible = true
            networkTv.text = token.chainName
            
            typeLl.isVisible = true
            typeTv.text = when (transaction.transactionType) {
                TransactionType.TRANSFER_OUT.value -> getString(R.string.Send)
                TransactionType.TRANSFER_IN.value -> getString(R.string.Receive)
                TransactionType.APPROVAL.value -> getString(R.string.Approval)
                TransactionType.SWAP.value -> getString(R.string.Swap)
                else -> transaction.transactionType
            }

            if (transaction.transactionType == TransactionType.SWAP.value && transaction.senders.isNotEmpty()) {
                assetChangesLl.visibility = View.VISIBLE
                assetChangesContainer.setContent {
                    AssetChangesList(
                        senders = transaction.senders,
                        receivers = transaction.receivers,
                        fetchToken = { assetId ->
                            web3ViewModel.web3TokenItemById(assetId)
                        }
                    )
                }
            } else if (transaction.transactionType == TransactionType.APPROVAL.value) {
                assetChangesLl.visibility = View.VISIBLE
                assetChangesTitle.setText(R.string.TOKEN_ACCESS_APPROVAL)
                
                assetChangesContainer.setContent {
                    AssetChangesList(
                        senders = transaction.senders,
                        receivers = transaction.receivers,
                        fetchToken = { assetId ->
                            web3ViewModel.web3TokenItemById(assetId)
                        },
                        approvals = transaction.approvals
                    )
                }
            } else {
                assetChangesLl.visibility = View.GONE
            }
        }
    }

    private fun tokenClick(transaction: Web3TransactionItem) {
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
                val url =
                    "${Constants.API.URL}external/explore/${token.chainId}/transactions/${transaction.transactionHash}"
                context?.openUrl(url)
                bottomSheet.dismiss()
            }
            cancel.setOnClickListener { bottomSheet.dismiss() }
        }

        bottomSheet.show()
    }
}
