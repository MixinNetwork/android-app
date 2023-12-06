package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentTransactionBinding
import one.mixin.android.databinding.ViewBadgeCircleImageBinding
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navigate
import one.mixin.android.extension.navigateUp
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat2
import one.mixin.android.extension.textColor
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.Ticker
import one.mixin.android.vo.safe.SafeSnapshotType
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal

interface TransactionInterface {
    fun initView(
        fragment: Fragment,
        contentBinding: FragmentTransactionBinding,
        lifecycleScope: CoroutineScope,
        walletViewModel: WalletViewModel,
        assetId: String?,
        snapshotId: String?,
        tokenItem: TokenItem?,
        snapshotItem: SnapshotItem?,
    ) {
        contentBinding.titleView.rightAnimator.visibility = View.GONE
        if (snapshotItem == null || tokenItem == null) {
            if (snapshotId != null && assetId != null) {
                lifecycleScope.launch {
                    val asset = walletViewModel.simpleAssetItem(assetId)
                    val snapshot = walletViewModel.snapshotLocal(assetId, snapshotId)
                    if (asset == null || snapshot == null) {
                        toast(R.string.Data_error)
                    } else {
                        contentBinding.avatar.setOnClickListener {
                            clickAvatar(fragment, asset)
                        }
                        updateUI(fragment, contentBinding, asset, snapshot)
                        fetchThatTimePrice(
                            fragment,
                            lifecycleScope,
                            walletViewModel,
                            contentBinding,
                            asset.assetId,
                            snapshot,
                        )
                        refreshNoTransactionHashWithdrawal(
                            fragment,
                            contentBinding,
                            lifecycleScope,
                            walletViewModel,
                            snapshot,
                            asset,
                        )
                    }
                }
            } else {
                toast(R.string.Data_error)
            }
        } else {
            contentBinding.avatar.setOnClickListener {
                clickAvatar(fragment, tokenItem)
            }
            updateUI(fragment, contentBinding, tokenItem, snapshotItem)
            fetchThatTimePrice(
                fragment,
                lifecycleScope,
                walletViewModel,
                contentBinding,
                tokenItem.assetId,
                snapshotItem,
            )
        }
    }

    private fun clickAvatar(
        fragment: Fragment,
        asset: TokenItem,
    ) {
        val curActivity = fragment.requireActivity()
        if (curActivity is WalletActivity) {
            if ((fragment.findNavController().previousBackStackEntry?.destination as FragmentNavigator.Destination?)?.label == AllTransactionsFragment.TAG) {
                fragment.view?.navigate(
                    R.id.action_transaction_fragment_to_transactions,
                    Bundle().apply { putParcelable(TransactionsFragment.ARGS_ASSET, asset) },
                )
            } else {
                fragment.view?.navigateUp()
            }
        } else {
            WalletActivity.show(curActivity, asset, false)
        }
    }

    private fun fetchThatTimePrice(
        fragment: Fragment,
        lifecycleScope: CoroutineScope,
        walletViewModel: WalletViewModel,
        contentBinding: FragmentTransactionBinding,
        assetId: String,
        snapshot: SnapshotItem,
    ) = lifecycleScope.launch {
        if (checkDestroyed(fragment)) return@launch

        contentBinding.thatVa.displayedChild = POS_PB
        handleMixinResponse(
            invokeNetwork = { walletViewModel.ticker(assetId, snapshot.createdAt) },
            successBlock = {
                if (checkDestroyed(fragment)) return@handleMixinResponse

                val ticker = it.data
                if (ticker != null) {
                    updateTickerText(contentBinding, ticker, fragment, snapshot)
                } else {
                    showRetry(
                        fragment,
                        lifecycleScope,
                        walletViewModel,
                        contentBinding,
                        assetId,
                        snapshot,
                    )
                }
            },
            exceptionBlock = {
                showRetry(
                    fragment,
                    lifecycleScope,
                    walletViewModel,
                    contentBinding,
                    assetId,
                    snapshot,
                )
                return@handleMixinResponse false
            },
            failureBlock = {
                showRetry(
                    fragment,
                    lifecycleScope,
                    walletViewModel,
                    contentBinding,
                    assetId,
                    snapshot,
                )
                return@handleMixinResponse false
            },
        )
    }

    fun updateTickerText(
        contentBinding: FragmentTransactionBinding,
        ticker: Ticker,
        fragment: Fragment,
        snapshot: SnapshotItem,
    ) {
        if (checkDestroyed(fragment)) return

        contentBinding.thatVa.displayedChild = POS_TEXT
        contentBinding.thatTv.apply {
            text =
                if (ticker.priceUsd == "0") {
                    fragment.getString(R.string.value_then, fragment.getString(R.string.NA))
                } else {
                    val amount =
                        (BigDecimal(snapshot.amount).abs() * ticker.priceFiat()).numberFormat2()
                    val pricePerUnit =
                        if (BuildConfig.DEBUG) {
                            "(${Fiats.getSymbol()}${
                                ticker.priceFiat().priceFormat2()
                            }/${snapshot.assetSymbol})"
                        } else {
                            ""
                        }
                    fragment.getString(
                        R.string.value_then,
                        "${Fiats.getSymbol()}$amount $pricePerUnit",
                    )
                }
            fragment.context?.let { c ->
                setTextColor(c.colorFromAttribute(R.attr.text_minor))
                setOnClickListener {
                    if (checkDestroyed(fragment)) return@setOnClickListener

                    val balloon =
                        createBalloon(c) {
                            setArrowSize(10)
                            setHeight(45)
                            setCornerRadius(4f)
                            setAlpha(0.9f)
                            setAutoDismissDuration(3000L)
                            setBalloonAnimation(BalloonAnimation.FADE)
                            setText(fragment.getString(R.string.wallet_transaction_that_time_value_tip))
                            setTextColorResource(R.color.white)
                            setPaddingLeft(10)
                            setPaddingRight(10)
                            setBackgroundColorResource(R.color.colorLightBlue)
                            setLifecycleOwner(fragment.viewLifecycleOwner)
                        }
                    balloon.showAlignTop(this)
                }
            }
        }
    }

    private fun showRetry(
        fragment: Fragment,
        lifecycleScope: CoroutineScope,
        walletViewModel: WalletViewModel,
        contentBinding: FragmentTransactionBinding,
        assetId: String,
        snapshot: SnapshotItem,
    ) {
        if (checkDestroyed(fragment)) return

        contentBinding.apply {
            thatVa.displayedChild = POS_TEXT
            thatTv.apply {
                text = fragment.getString(R.string.Click_to_retry)
                setTextColor(fragment.resources.getColor(R.color.colorDarkBlue, null))
                setOnClickListener {
                    fetchThatTimePrice(
                        fragment,
                        lifecycleScope,
                        walletViewModel,
                        contentBinding,
                        assetId,
                        snapshot,
                    )
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(
        fragment: Fragment,
        contentBinding: FragmentTransactionBinding,
        asset: TokenItem,
        snapshot: SnapshotItem,
    ) {
        if (checkDestroyed(fragment)) return

        contentBinding.apply {
            val amountVal = snapshot.amount.toFloatOrNull()
            val isPositive = if (amountVal == null) false else amountVal > 0
            ViewBadgeCircleImageBinding.bind(contentBinding.avatar).apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }

            val amountText =
                if (isPositive) {
                    "+${snapshot.amount.numberFormat()}"
                } else {
                    snapshot.amount.numberFormat()
                }
            val amountColor =
                fragment.resources.getColor(
                    when {
                        snapshot.type == SafeSnapshotType.pending.name -> {
                            R.color.wallet_text_gray
                        }
                        isPositive -> {
                            R.color.wallet_green
                        }
                        else -> {
                            R.color.wallet_pink
                        }
                    },
                    null,
                )
            val symbolColor = fragment.requireContext().colorFromAttribute(R.attr.text_primary)
            valueTv.text = buildAmountSymbol(fragment.requireContext(), amountText, asset.symbol, amountColor, symbolColor)
            val amount = (BigDecimal(snapshot.amount).abs() * asset.priceFiat()).numberFormat2()
            val pricePerUnit =
                "(${Fiats.getSymbol()}${asset.priceFiat().priceFormat2()}/${snapshot.assetSymbol})"

            valueAsTv.text =
                fragment.getString(
                    R.string.value_now,
                    "${Fiats.getSymbol()}$amount $pricePerUnit",
                )
            transactionIdTv.text = snapshot.snapshotId
            transactionHashLayout.isVisible = !snapshot.transactionHash.isNullOrBlank()
            transactionHashTv.text = snapshot.transactionHash
            dateTv.text = snapshot.createdAt.fullDate()
            memoLl.isVisible = snapshot.formatMemo != null
            memoTv.text = snapshot.formatMemo?.utf ?: snapshot.formatMemo?.hex
            memoLayout.setOnClickListener {
                val memo = snapshot.formatMemo ?: return@setOnClickListener
                MemoBottomSheetDialogFragment.newInstance(memo).showNow(fragment.parentFragmentManager, MemoBottomSheetDialogFragment.TAG)
            }
            val type = snapshot.simulateType()
            when (type) {
                SafeSnapshotType.transfer -> {
                    fromTv.text =
                        if (snapshot.opponentId.isBlank()) {
                            fromTv.textColor = fromTv.context.colorFromAttribute(R.attr.text_minor)
                            "N/A"
                        } else {
                            fromTv.textColor = fromTv.context.colorFromAttribute(R.attr.text_primary)
                            snapshot.opponentFullName
                        }
                    if (isPositive) {
                        fromTitle.text = fragment.getString(R.string.From)
                    } else {
                        fromTitle.text = fragment.getString(R.string.To)
                    }
                }
                SafeSnapshotType.pending -> {
                    memoLl.isVisible = false
                    transactionIdLl.isVisible = false
                    transactionHashLayout.isVisible = false
                    confirmationLl.isVisible = true
                    confirmationTv.text =
                        fragment.requireContext().resources.getQuantityString(
                            R.plurals.pending_confirmation,
                            snapshot.confirmations ?: 0,
                            snapshot.confirmations ?: 0,
                            snapshot.assetConfirmations,
                        )
                    if (snapshot.deposit != null) {
                        hashLl.isVisible = true
                        hashTitle.text = fragment.getString(R.string.deposit_hash)
                        hashTv.text = snapshot.deposit.depositHash
                        if (snapshot.deposit.sender.isNotBlank()) {
                            fromTv.text = snapshot.deposit.sender
                        } else {
                            fromTv.text = "N/A"
                        }
                    }
                }
                SafeSnapshotType.deposit -> {
                    memoLl.isVisible = false
                    if (snapshot.deposit != null) {
                        hashLl.isVisible = true
                        hashTitle.text = fragment.getString(R.string.deposit_hash)
                        hashTv.text = snapshot.deposit.depositHash
                        if (snapshot.deposit.sender.isNotBlank()) {
                            fromTv.text = snapshot.deposit.sender
                        } else {
                            fromTv.text = "N/A"
                        }
                    }
                }
                SafeSnapshotType.withdrawal -> {
                    memoLl.isVisible = false
                    fromTitle.text = fragment.getString(R.string.To)
                    if (snapshot.withdrawal != null) {
                        hashLl.isVisible = true
                        hashTitle.text = fragment.getString(R.string.withdrawal_hash)
                        if (snapshot.withdrawal.withdrawalHash.isBlank()) {
                            hashTv.text = fragment.getString(R.string.withdrawal_pending)
                        } else {
                            hashTv.text = snapshot.withdrawal.withdrawalHash
                        }
                        fromTitle.text = fragment.getString(R.string.To)
                        if (snapshot.withdrawal.receiver.isNotBlank()) {
                            fromTv.text = snapshot.withdrawal.receiver
                        } else {
                            fromTv.text = "N/A"
                        }
                    }
                }
            }
        }
    }

    private fun refreshNoTransactionHashWithdrawal(
        fragment: Fragment,
        contentBinding: FragmentTransactionBinding,
        lifecycleScope: CoroutineScope,
        walletViewModel: WalletViewModel,
        snapshot: SnapshotItem,
        asset: TokenItem,
    ) {
        if (snapshot.type == SafeSnapshotType.pending.name) return

        val amountVal = snapshot.amount.toFloatOrNull()
        val isPositive = if (amountVal == null) false else amountVal > 0
        val isWithdrawal = snapshot.opponentId.isBlank() && !isPositive
        if (isWithdrawal && snapshot.withdrawal?.withdrawalHash?.isBlank() == true) {
            lifecycleScope.launch {
                walletViewModel.refreshSnapshot(snapshot.snapshotId)?.let {
                    updateUI(fragment, contentBinding, asset, snapshot)
                }
            }
        }
    }

    private fun checkDestroyed(fragment: Fragment) =
        if (fragment is DialogFragment) {
            !fragment.isAdded
        } else {
            fragment.viewDestroyed()
        }

    companion object {
        const val POS_PB = 0
        const val POS_TEXT = 1
    }
}
