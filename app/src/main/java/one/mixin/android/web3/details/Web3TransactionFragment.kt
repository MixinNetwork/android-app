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
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.databinding.FragmentWeb3TransactionBinding
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat2
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.SafeSnapshotType
import one.mixin.android.web3.details.Web3TransactionDetailsFragment.Companion.ARGS_TOKEN
import timber.log.Timber
import java.math.BigDecimal
import java.util.Locale

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
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.root.isClickable = true
        binding.apply {
            transactionIdTv.text = transaction.transactionHash
            transactionHashTv.text = transaction.transactionHash
            val amountColor = if (transaction.amount.toBigDecimal() > BigDecimal.ZERO) {
                R.color.wallet_green
            } else {
                R.color.wallet_pink
            }
            val symbolColor = requireContext().colorFromAttribute(R.attr.text_primary)
            valueTv.text = buildAmountSymbol(requireContext(), transaction.amount, transaction.symbol, amountColor, symbolColor)
            val amount = (BigDecimal(transaction.amount).abs() * token.priceFiat()).numberFormat2()
            val pricePerUnit =
                "(${Fiats.getSymbol()}${token.priceFiat().priceFormat2()}/${token.symbol})"
            valueAsTv.text =
                getString(
                    R.string.value_now,
                    "${Fiats.getSymbol()}$amount $pricePerUnit",
                )
            fromTv.text = transaction.sender
            toTv.text = transaction.receiver
            avatar.bg.loadImage(transaction.iconUrl, R.drawable.ic_avatar_place_holder)
            avatar.setOnClickListener {
                tokenClick(transaction)
            }
            val badge = transaction.iconUrl
            if (badge == null) {
                avatar.badge.isVisible = false
            } else {
                avatar.badge.isVisible = true
                avatar.badge.loadImage(badge, R.drawable.ic_avatar_place_holder)
            }
            dateTv.text = transaction.createdAt.fullDate()
            feeLl.isVisible = false
            statusLl.isVisible = false
        }
    }

    private fun tokenClick(transaction: Web3TransactionItem) {
        lifecycleScope.launch {
            // transaction.event?.let { event ->
                // Todo
                // if (event.address == token.assetKey) {
                //     requireActivity().onBackPressedDispatcher.onBackPressed()
                //     return@launch
                // }
                // val dialog =
                    // indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                    //     setCancelable(false)
                    // }

                // try {
                //     web3ViewModel.web3Token(
                //         chain, event.chainId, event.address
                //     ) ?: return@launch
                //     dialog.dismiss()
                //     RxBus.publish(event)
                // } catch (e: Exception) {
                //     Timber.e(e)
                //     dialog.dismiss()
                //     return@launch
                // }
            // }
        }
    }
}
