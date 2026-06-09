package one.mixin.android.ui.wallet.adapter

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemTransactionHeaderBinding
import one.mixin.android.databinding.ItemWalletTransactionsBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.textColor
import one.mixin.android.extension.timeAgoDay
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.SafeSnapshotType
import one.mixin.android.widget.linktext.RoundBackgroundColorSpan

open class SnapshotHolder(
    itemView: View,
    layout: Boolean = false,
    compact: Boolean = false,
) : NormalHolder(itemView) {
    private val binding = ItemWalletTransactionsBinding.bind(itemView)

    init {
        if (layout) {
            binding.avatar.updateLayoutParams {
                (this as MarginLayoutParams).apply {
                    marginEnd = 16.dp
                    marginStart = 16.dp
                }
            }
            binding.symbolTv.updateLayoutParams {
                (this as MarginLayoutParams).apply {
                    marginEnd = 16.dp
                }
            }
        }
        if (compact) {
            itemView.updateLayoutParams {
                height = RelativeLayout.LayoutParams.WRAP_CONTENT
            }
            itemView.setPadding(0, 4.dp, 0, 4.dp)
            binding.avatar.updateLayoutParams<MarginLayoutParams> {
                marginEnd = 14.dp
                marginStart = 16.dp
            }
        }
    }

    open fun bind(
        snapshot: SnapshotItem,
        listener: OnSnapshotListener?,
    ) {
        val isPositive = snapshot.amount.toFloat() > 0
        when (val type = snapshot.simulateType()) {
            SafeSnapshotType.snapshot -> {
                if (snapshot.opponentId.isBlank()) {
                    binding.name.text = "N/A"
                    binding.name.textColor = binding.root.context.colorFromAttribute(R.attr.text_assist)
                    binding.avatar.setAnonymous()
                } else if (snapshot.opponentId.startsWith("XIN", true)) {
                    binding.name.text = snapshot.opponentId.formatPublicKey(limit = 14, prefixLen = 8, suffixLen = 6)
                    binding.name.textColor = binding.root.context.colorFromAttribute(R.attr.text_assist)
                    binding.avatar.setAnonymous()
                } else {
                    binding.name.text = snapshot.opponentFullName
                    binding.name.textColor = binding.root.context.colorFromAttribute(R.attr.text_primary)
                    binding.avatar.setInfo(snapshot.opponentFullName, snapshot.avatarUrl, snapshot.opponentId)
                    binding.avatar.setOnClickListener {
                        listener?.onUserClick(snapshot.opponentId)
                    }
                }
                binding.bg.setConfirmation(0, 0)
            }

            SafeSnapshotType.pending -> {
                val maxConfirmations = snapshot.assetConfirmations.coerceAtLeast(0)
                val currentConfirmations = (snapshot.confirmations ?: 0)
                    .coerceAtLeast(0)
                    .coerceAtMost(maxConfirmations)
                binding.name.textColor = binding.root.context.colorFromAttribute(R.attr.text_primary)
                binding.name.text = itemView.context.resources.getQuantityString(
                    R.plurals.pending_confirmation,
                    currentConfirmations,
                    currentConfirmations,
                    maxConfirmations
                )
                binding.avatar.setDeposit()
                binding.bg.setConfirmation(maxConfirmations, currentConfirmations)
            }

            else -> {
                if (type == SafeSnapshotType.deposit) {
                    binding.avatar.setDeposit()
                    val sender = snapshot.deposit?.sender
                    binding.name.text =
                        if (sender.isNullOrBlank()) {
                            "N/A"
                        } else {
                            sender.formatTransactionHashIfNeeded()
                        }
                } else {
                    binding.avatar.setWithdrawal()
                    val receiver = snapshot.withdrawal?.receiver
                    val label = snapshot.label
                    if (receiver.isNullOrBlank()) {
                        binding.name.text = "N/A"
                    } else if (label.isNullOrBlank()) {
                        binding.name.text = receiver.formatTransactionHashIfNeeded()
                    } else {
                        val fullText = "${receiver.formatTransactionHashIfNeeded()} $label"
                        val spannableString = SpannableString(fullText)
                        val start = fullText.lastIndexOf(label)
                        val end = start + label.length

                        val backgroundColor: Int = Color.parseColor("#8DCC99")
                        val backgroundColorSpan = RoundBackgroundColorSpan(backgroundColor, Color.WHITE)
                        spannableString.setSpan(RelativeSizeSpan(0.8f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannableString.setSpan(backgroundColorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        binding.name.text = spannableString
                    }
                }
                binding.name.textColor = binding.root.context.colorFromAttribute(R.attr.text_primary)
                binding.bg.setConfirmation(0, 0)
            }
        }

        binding.value.text =
            if (snapshot.inscriptionHash.isNullOrEmpty()) {
                if (isPositive) {
                    "+${snapshot.amount.numberFormat()}"
                } else {
                    snapshot.amount.numberFormat()
                }
            } else {
                if (isPositive) {
                    "+1"
                } else {
                    "-1"
                }
            }
        binding.value.textColor = when {
            snapshot.type == SafeSnapshotType.pending.name -> binding.root.context.colorFromAttribute(R.attr.text_primary)
            // Pending withdrawal
            snapshot.isPendingWithdrawal() -> binding.root.context.colorFromAttribute(R.attr.text_primary)
            isPositive -> binding.root.context.getColor(R.color.wallet_green)
            else -> binding.root.context.getColor(R.color.wallet_pink)
        }

        if (snapshot.inscriptionHash.isNullOrBlank()) {
            binding.symbolIv.isVisible = false
            binding.symbolTv.isVisible = true
            (binding.value.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.START_OF, R.id.symbol_tv)
            (binding.value.layoutParams as RelativeLayout.LayoutParams).marginEnd = 6.dp
            binding.symbolTv.text = snapshot.assetSymbol
        } else {
            binding.symbolIv.isVisible = true
            binding.symbolTv.isVisible = false
            (binding.value.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.START_OF, R.id.symbol_iv)
            (binding.value.layoutParams as RelativeLayout.LayoutParams).marginEnd = 8.dp
            binding.symbolIv.render(snapshot)
        }

        itemView.setOnClickListener {
            listener?.onNormalItemClick(snapshot)
        }
    }
}

private fun String.formatTransactionHashIfNeeded(): String {
    val normalized = removePrefix("0x").removePrefix("0X")
    return if (normalized.length == 64 && normalized.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
        formatPublicKey(limit = 14, prefixLen = 8, suffixLen = 6)
    } else {
        formatPublicKey()
    }
}

class SnapshotHeaderViewHolder(itemView: View, layout: Boolean = false) : RecyclerView.ViewHolder(itemView) {
    private val binding = ItemTransactionHeaderBinding.bind(itemView)

    init {
        if (layout) binding.dateTv.setPadding(16.dp, 0, 16.dp, 0)
    }

    fun bind(time: String) {
        binding.dateTv.timeAgoDay(time)
    }
}

interface OnSnapshotListener {
    fun <T> onNormalItemClick(item: T)

    fun onUserClick(userId: String)

    fun onMoreClick()
}
