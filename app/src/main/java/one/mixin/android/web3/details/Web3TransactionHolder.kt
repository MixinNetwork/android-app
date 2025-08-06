package one.mixin.android.web3.details

import android.annotation.SuppressLint
import android.util.TypedValue
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isVisible
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.firstOrNull
import one.mixin.android.R
import one.mixin.android.databinding.ItemWeb3TokenHeaderBinding
import one.mixin.android.databinding.ItemWeb3TransactionsBinding
import one.mixin.android.db.web3.vo.AssetChange
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.TransactionType
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.numberFormat12
import one.mixin.android.extension.textColorResource
import one.mixin.android.ui.home.web3.StakeAccountSummary
import one.mixin.android.ui.home.web3.Web3ViewModel

class Web3TransactionHolder(
    val binding: ItemWeb3TransactionsBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun formatAmountWithSign(amount: String?, positive: Boolean): String {
        if (amount.isNullOrEmpty()) return "N/A"
        val formattedAmount = amount.numberFormat12()
        return if (positive) {
            if (formattedAmount.startsWith("+")) formattedAmount else "+$formattedAmount"
        } else {
            if (formattedAmount.startsWith("-")) formattedAmount else "-$formattedAmount"
        }
    }

    @SuppressLint("SetTextI18s")
    fun bind(transaction: Web3TransactionItem) {
        binding.apply {
            val hash = transaction.transactionHash
            name.text = if (hash.length > 14) {
                "${hash.substring(0, 8)}...${hash.substring(hash.length - 6)}"
            } else {
                hash
            }
            icSpam.isVisible = transaction.isNotVerified()
            val amount = transaction.getFormattedAmount()
            when {
                transaction.status == TransactionStatus.PENDING.value || transaction.status == TransactionStatus.NOT_FOUND.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    amountAnimator.displayedChild = 0
                    value.setTextColor(root.context.colorAttr(R.attr.text_assist))
                    value.text = ""
                    symbolTv.text =
                        itemView.context.getString(if (transaction.status == TransactionStatus.NOT_FOUND.value) R.string.Expired else R.string.Pending)
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.UNKNOWN.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    amountAnimator.displayedChild = 0
                    value.setTextColor(root.context.colorAttr(R.attr.text_assist))
                    value.text = ""
                    symbolTv.text = itemView.context.getString(R.string.Unknown)
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.TRANSFER_IN.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    if (transaction.senders.size > 1 || transaction.receivers.size > 1) {
                        amountAnimator.displayedChild = 1
                        val assetChanges = (transaction.receivers + transaction.senders).take(3)
                        binding.doubleLineComposeView.setContent {
                            AmountList(assetChanges = assetChanges, senders = transaction.senders)
                        }
                    } else {
                        amountAnimator.displayedChild = 0
                        value.textColorResource = R.color.wallet_green
                        value.text = formatAmountWithSign(amount, true)
                        symbolTv.text = transaction.receiveAssetSymbol ?: ""
                    }
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.TRANSFER_OUT.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    if (transaction.senders.size > 1 || transaction.receivers.size > 1) {
                        amountAnimator.displayedChild = 1
                        val assetChanges = (transaction.receivers + transaction.senders).take(3)
                        binding.doubleLineComposeView.setContent {
                            AmountList(assetChanges = assetChanges, senders = transaction.senders)
                        }
                    } else {
                        amountAnimator.displayedChild = 0
                        value.textColorResource = R.color.wallet_pink
                        value.text = formatAmountWithSign(amount, false)
                        symbolTv.text = transaction.sendAssetSymbol ?: ""
                    }
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.SWAP.value -> {
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    if (transaction.senders.isNotEmpty()) {
                        amountAnimator.displayedChild = 1
                        val assetChanges = (transaction.receivers + transaction.senders).take(3)
                        binding.doubleLineComposeView.setContent {
                            AmountList(assetChanges = assetChanges, senders = transaction.senders)
                        }
                    } else {
                        amountAnimator.displayedChild = 0
                        value.textColorResource = R.color.wallet_green
                        value.text = formatAmountWithSign(amount, true)
                        symbolTv.text = transaction.receiveAssetSymbol ?: ""
                    }
                    avatar.loadUrl(transaction)
                }
                transaction.transactionType == TransactionType.APPROVAL.value -> {
                    amountAnimator.displayedChild = 0
                    value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    avatar.loadUrl(transaction)

                    val approvals = transaction.approvals
                    if (approvals != null && approvals.isNotEmpty()) {
                        val approvalAssetChange = approvals[0]
                        val isUnlimited = approvalAssetChange.type == "unlimited"

                        if (isUnlimited) {
                            value.textColorResource = R.color.wallet_pink
                            value.text = itemView.context.getString(R.string.unlimited)
                            symbolTv.text = transaction.sendAssetSymbol ?: ""
                        } else {
                            value.textColorResource = R.color.wallet_pink
                            value.text = itemView.context.getString(R.string.Approved)
                            symbolTv.text = "${approvalAssetChange.amount} ${transaction.sendAssetSymbol ?: ""}"
                        }
                    } else {
                        value.textColorResource = R.color.wallet_pink
                        value.text = itemView.context.getString(R.string.Approved)
                        symbolTv.text = transaction.sendAssetSymbol ?: ""
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
) {
    val holder = LocalView.current.tag as? Web3TransactionHolder
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.wrapContentWidth()
    ) {
        assetChanges.forEachIndexed { index, assetChange ->
            val isSender = senders.contains(assetChange)
            val amount = holder?.formatAmountWithSign(assetChange.amount, !isSender) ?: assetChange.amount

            AmountRow(
                amount = amount,
                symbol = assetChange.symbol ?: "",
                isSender = isSender
            )
            if (index < assetChanges.size - 1) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
fun AmountRow(amount: String, symbol: String, isSender: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.wrapContentWidth()
    ) {
        Text(
            text = "${if (isSender) "+" else "-"}$amount",
            color = colorResource(id = if (isSender) R.color.wallet_pink else R.color.wallet_green),
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
