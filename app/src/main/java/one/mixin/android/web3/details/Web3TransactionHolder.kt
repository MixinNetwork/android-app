package one.mixin.android.web3.details

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemWeb3TokenHeaderBinding
import one.mixin.android.databinding.ItemWeb3TransactionsBinding
import one.mixin.android.db.web3.vo.AssetChange
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.numberFormat8
import one.mixin.android.ui.home.web3.StakeAccountSummary
import one.mixin.android.extension.dp as dip

class Web3TransactionHolder(
    val binding: ItemWeb3TransactionsBinding,
    private val compact: Boolean = false,
    private val compactAvatarStartMargin: Int = 16.dip,
) : RecyclerView.ViewHolder(binding.root) {
    init {
        if (compact) {
            binding.root.updateLayoutParams<ViewGroup.LayoutParams> {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            binding.root.setPadding(0, 4.dip, 0, 4.dip)
            binding.avatarFl.updateLayoutParams<MarginLayoutParams> {
                marginStart = compactAvatarStartMargin
                marginEnd = 14.dip
            }
            binding.amountAnimator.updateLayoutParams<MarginLayoutParams> {
                marginEnd = 16.dip
            }
        }
    }

    @SuppressLint("SetTextI18s")
    fun bind(transaction: Web3TransactionItem) {
        binding.apply {
            val hash = transaction.transactionHash
            name.text = hash.formatPublicKey(limit = 14, prefixLen = 8, suffixLen = 6)
            icSpam.isVisible = transaction.isNotVerified()
            val amount = transaction.getFormattedAmount()
            when {
                !transaction.hasDisplayAssetChanges() && transaction.transactionType != TransactionType.APPROVAL.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    amountAnimator.displayedChild = 0
                    value.setTextColor(root.context.colorAttr(R.attr.text_assist))
                    value.text = ""
                    symbolTv.text =
                        itemView.context.getString(when (transaction.status) {
                            TransactionStatus.PENDING.value -> R.string.Pending
                            TransactionStatus.NOT_FOUND.value -> R.string.Expired
                            TransactionStatus.FAILED.value -> R.string.Failed
                            else -> R.string.Unknown
                        })
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.TRANSFER_IN.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    if (transaction.receivers.size > 1) {
                        amountAnimator.displayedChild = 1
                        val assetChanges = transaction.receivers.take(3)
                        binding.doubleLineComposeView.setContent {
                            AmountList(assetChanges = assetChanges, senders = transaction.senders, status = transaction.status)
                        }
                    } else {
                        amountAnimator.displayedChild = 0
                        value.setTextColor(root.context.web3AmountColor(transaction.status, amount, true))
                        value.text = formatWeb3AmountWithSign(amount.numberFormat8(), true)
                        symbolTv.text = getFormattedSymbol(transaction.receiveAssetSymbol) ?: ""
                    }
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.TRANSFER_OUT.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    if (transaction.senders.size > 1) {
                        amountAnimator.displayedChild = 1
                        val assetChanges = (transaction.senders).take(3)
                        binding.doubleLineComposeView.setContent {
                            AmountList(assetChanges = assetChanges, senders = transaction.senders, status = transaction.status)
                        }
                    } else {
                        amountAnimator.displayedChild = 0
                        value.setTextColor(root.context.web3AmountColor(transaction.status, amount, false))
                        value.text = formatWeb3AmountWithSign(amount.numberFormat8(), false)
                        symbolTv.text = getFormattedSymbol(transaction.sendAssetSymbol) ?: ""
                    }
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.SWAP.value || transaction.transactionType == TransactionType.UNKNOWN.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    if (transaction.senders.isNotEmpty() || transaction.receivers.size > 1 || transaction.transactionType == TransactionType.UNKNOWN.value) {
                        amountAnimator.displayedChild = 1
                        val assetChanges = (transaction.receivers + transaction.senders).take(3)
                        binding.doubleLineComposeView.setContent {
                            AmountList(assetChanges = assetChanges, senders = transaction.senders, status = transaction.status)
                        }
                    } else {
                        amountAnimator.displayedChild = 0
                        value.setTextColor(root.context.web3AmountColor(transaction.status, amount, true))
                        value.text = formatWeb3AmountWithSign(amount.numberFormat8(), true)
                        symbolTv.text = getFormattedSymbol(transaction.receiveAssetSymbol) ?: ""
                    }
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.APPROVAL.value -> {
                    amountAnimator.displayedChild = 0
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    avatar.loadUrl(transaction)

                    value.setTextColor(if (transaction.status == TransactionStatus.SUCCESS.value) root.context.getColor(R.color.wallet_pink) else root.context.colorAttr(R.attr.text_primary))
                    val approvals = transaction.approvals
                    if (approvals != null && approvals.isNotEmpty()) {
                        val approvalAssetChange = approvals[0]
                        val isUnlimited = approvalAssetChange.type == "unlimited"

                        if (isUnlimited) {
                            value.text = itemView.context.getString(R.string.unlimited)
                            symbolTv.text = getFormattedSymbol(transaction.sendAssetSymbol) ?: ""
                        } else {
                            value.text = itemView.context.getString(R.string.Approved)
                            symbolTv.text = "${approvalAssetChange.amount} ${getFormattedSymbol(transaction.sendAssetSymbol) ?: ""}"
                        }
                    } else {
                        value.text = itemView.context.getString(R.string.Approved)
                        symbolTv.text = getFormattedSymbol(transaction.sendAssetSymbol) ?: ""
                    }
                }
                else -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    amountAnimator.displayedChild = 0
                    avatar.loadUrl(transaction)
                    value.setTextColor(root.context.colorAttr(R.attr.text_primary))
                    value.text = ""
                    symbolTv.text = ""
                }
            }
            when (transaction.status) {
                TransactionStatus.SUCCESS.value -> {
                    badge.setImageResource(R.drawable.ic_web3_status_success)
                }

                TransactionStatus.PENDING.value -> {
                    badge.setImageResource(R.drawable.ic_web3_status_pending)
                }

                else -> {
                    badge.setImageResource(R.drawable.ic_web3_status_failed)
                }
            }
        }
    }

    private fun getFormattedSymbol(symbol: String?): String? {
        symbol ?: return null
        return if (symbol.length > 8) "${symbol.take(8)}…" else symbol
    }
}

class Web3HeaderHolder(val binding: ItemWeb3TokenHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        token: Web3TokenItem,
        summary: StakeAccountSummary?,
        onClickListener: ((Int) -> Unit)?,
    ) {
        binding.header.setToken(token)
        binding.header.setOnClickAction(onClickListener)
        binding.header.showStake(summary)
    }

    fun enableSwap() {
        binding.header.enableSwap()
    }
}

@Composable
fun AmountList(
    assetChanges: List<AssetChange>,
    senders: List<AssetChange>,
    status: String,
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.wrapContentWidth()
    ) {
        assetChanges.forEachIndexed { index, assetChange ->
            val isSender = senders.contains(assetChange)

            AmountRow(
                amount = assetChange.amount,
                symbol = assetChange.symbol ?: "",
                isSender = isSender,
                status = status,
            )
            if (index < assetChanges.size - 1) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
fun AmountRow(amount: String, symbol: String, isSender: Boolean, status: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.wrapContentWidth()
    ) {
        Text(
            text = formatWeb3AmountWithSign(amount.numberFormat8(), !isSender),
            color = Color(LocalContext.current.web3AmountColor(status, amount, !isSender)),
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.mixin_font)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = symbol,
            color = Color(LocalContext.current.colorAttr(R.attr.text_primary)),
            fontSize = 10.sp,
            modifier = Modifier
                .widthIn(min = 20.dp, max = 200.dp)
                .offset(y = (-1).dp)
        )
    }
}
