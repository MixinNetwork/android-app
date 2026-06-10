package one.mixin.android.ui.oldwallet

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentOldTransactionBinding
import one.mixin.android.databinding.ViewBadgeCircleImageBinding
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat2
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.session.Session
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.SnapshotType
import one.mixin.android.vo.Ticker
import one.mixin.android.widget.DebugClickListener
import java.math.BigDecimal

interface TransactionInterface {
    fun initView(
        fragment: Fragment,
        contentBinding: FragmentOldTransactionBinding,
        lifecycleScope: CoroutineScope,
        walletViewModel: WalletViewModel,
        assetId: String?,
        snapshotId: String?,
        assetItem: AssetItem?,
        snapshotItem: SnapshotItem?,
    ) {
        contentBinding.titleView.rightAnimator.visibility = View.GONE
        if (snapshotItem == null || assetItem == null) {
            if (snapshotId != null && assetId != null) {
                lifecycleScope.launch {
                    val asset = walletViewModel.simpleAssetItem(assetId)
                    val snapshot = walletViewModel.snapshotLocal(assetId, snapshotId)
                    if (asset == null || snapshot == null) {
                        toast(R.string.Data_error)
                    } else {
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
            contentBinding.transactionIdTitleTv.setOnClickListener(
                object : DebugClickListener() {
                    override fun onDebugClick() {
                        contentBinding.traceLl.visibility = View.VISIBLE
                    }

                    override fun onSingleClick() {
                    }
                },
            )
            updateUI(fragment, contentBinding, assetItem, snapshotItem)
            fetchThatTimePrice(
                fragment,
                lifecycleScope,
                walletViewModel,
                contentBinding,
                assetItem.assetId,
                snapshotItem,
            )
            refreshNoTransactionHashWithdrawal(
                fragment,
                contentBinding,
                lifecycleScope,
                walletViewModel,
                snapshotItem,
                assetItem,
            )
        }
    }

    private fun fetchThatTimePrice(
        fragment: Fragment,
        lifecycleScope: CoroutineScope,
        walletViewModel: WalletViewModel,
        contentBinding: FragmentOldTransactionBinding,
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
        contentBinding: FragmentOldTransactionBinding,
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
                setTextColor(c.colorFromAttribute(R.attr.text_assist))
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
        contentBinding: FragmentOldTransactionBinding,
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
        contentBinding: FragmentOldTransactionBinding,
        asset: AssetItem,
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
                        snapshot.type == SnapshotType.pending.name -> {
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
            transactionTypeTv.text = getSnapshotType(fragment, snapshot.type)
            memoTv.text = snapshot.memo
            openingBalanceLayout.isVisible = !snapshot.openingBalance.isNullOrBlank()
            openingBalanceTv.text = "${snapshot.openingBalance} ${asset.symbol}"
            closingBalanceLayout.isVisible = !snapshot.closingBalance.isNullOrBlank()
            closingBalanceTv.text = "${snapshot.closingBalance} ${asset.symbol}"
            snapshotHashLayout.isVisible = !snapshot.snapshotHash.isNullOrBlank()
            snapshotHashTv.text = snapshot.snapshotHash
            dateTv.text = snapshot.createdAt.fullDate()
            when (snapshot.type) {
                SnapshotType.deposit.name -> {
                    senderTitle.text = fragment.getString(R.string.From)
                    senderTv.text = snapshot.sender
                    receiverTitle.text = fragment.getString(R.string.transaction_Hash)
                    receiverTv.text = snapshot.transactionHash
                }
                SnapshotType.pending.name -> {
                    senderTitle.text = fragment.getString(R.string.From)
                    senderTv.text = snapshot.sender
                    receiverTitle.text = fragment.getString(R.string.transaction_Hash)
                    receiverTv.text = snapshot.transactionHash
                    transactionStatus.isVisible = true
                    transactionStatusTv.text =
                        fragment.requireContext().resources.getQuantityString(
                            R.plurals.pending_confirmation,
                            snapshot.confirmations ?: 0,
                            snapshot.confirmations ?: 0,
                            snapshot.assetConfirmations,
                        )
                }
                SnapshotType.transfer.name -> {
                    traceTv.text = snapshot.traceId
                    if (isPositive) {
                        senderTv.text = snapshot.opponentFullName
                        receiverTv.text = Session.getAccount()!!.fullName
                    } else {
                        senderTv.text = Session.getAccount()!!.fullName
                        receiverTv.text = snapshot.opponentFullName
                    }
                }
                else -> { // withdrawal, fee, rebate, raw
                    if (!asset.getTag().isNullOrEmpty()) {
                        receiverTitle.text = fragment.getString(R.string.Address)
                    } else {
                        receiverTitle.text = fragment.getString(R.string.To)
                    }
                    senderTitle.text = fragment.getString(R.string.transaction_Hash)
                    senderTv.text = snapshot.transactionHash
                    receiverTv.text = snapshot.receiver
                }
            }
        }
    }

    private fun refreshNoTransactionHashWithdrawal(
        fragment: Fragment,
        contentBinding: FragmentOldTransactionBinding,
        lifecycleScope: CoroutineScope,
        walletViewModel: WalletViewModel,
        snapshot: SnapshotItem,
        asset: AssetItem,
    ) {
        if (snapshot.type == SnapshotType.pending.name) return

        if (snapshot.snapshotHash.isNullOrBlank() || (snapshot.type == SnapshotType.withdrawal.name && snapshot.transactionHash.isNullOrBlank())) {
            lifecycleScope.launch {
                walletViewModel.refreshSnapshot(snapshot.snapshotId)?.let {
                    updateUI(fragment, contentBinding, asset, snapshot)
                }
            }
        }
    }

    fun getSnapshotType(
        fragment: Fragment,
        type: String,
    ): String {
        val s =
            when (type) {
                SnapshotType.transfer.name -> R.string.Transfer
                SnapshotType.deposit.name, SnapshotType.pending.name -> R.string.Deposit
                SnapshotType.withdrawal.name -> R.string.Withdrawal
                SnapshotType.fee.name -> R.string.Fee
                SnapshotType.rebate.name -> R.string.Rebate
                SnapshotType.raw.name -> R.string.Raw
                else -> R.string.NA
            }
        return fragment.requireContext().getString(s)
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
