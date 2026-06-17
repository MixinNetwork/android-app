package one.mixin.android.web3.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.AssetChange
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.numberFormat2
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

@Composable
fun AssetChangeItem(
    status: String,
    amount: String,
    symbol: String,
    iconUrl: String?,
    fiatValue: String? = null,
    isReceive: Boolean = false,
    isUnlimited: Boolean = false,
    isApproval: Boolean = false
) {
    val amountValue = if (isUnlimited) {
        stringResource(R.string.unlimited).replaceFirstChar { it.uppercase() }
    } else {
        try {
            BigDecimal(amount).stripTrailingZeros().toPlainString()
        } catch (e: Exception) {
            amount
        }
    }

    val prefix =
        if (amount.startsWith("+") || amount.startsWith("-")) "" else if (isReceive) "+" else "-"
    val textColor =
        if (status == TransactionStatus.PENDING.value) MixinAppTheme.colors.textPrimary
        else if (isApproval) MixinAppTheme.colors.walletRed
        else if (isReceive) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoilImage(
                model = iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier.size(18.dp),
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = if (isUnlimited) amountValue else "$prefix$amountValue ",
                fontSize = 14.sp,
                color = textColor
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = symbol,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textPrimary
            )
        }

        if (!fiatValue.isNullOrBlank()) {
            Text(
                text = fiatValue,
                fontSize = 12.sp,
                color = MixinAppTheme.colors.textAssist
            )
        }
    }
}

@Composable
fun AssetChangesList(
    status: String,
    senders: List<AssetChange>,
    receivers: List<AssetChange>,
    fetchToken: suspend (String) -> Web3TokenItem?,
    approvals: List<AssetChange>? = null
) {
    val scope = rememberCoroutineScope()
    val assetIds = remember(senders, receivers, approvals) {
        val ids = mutableListOf<String>()
        ids.addAll(senders.map { it.assetId })
        ids.addAll(receivers.map { it.assetId })
        approvals?.let { ids.addAll(it.map { approval -> approval.assetId }) }
        ids.distinct()
    }

    var tokens by remember { mutableStateOf<Map<String, Web3TokenItem>>(emptyMap()) }

    LaunchedEffect(assetIds) {
        scope.launch {
            assetIds.forEach { assetId ->
                val token = fetchToken(assetId)
                if (token != null) {
                    tokens = tokens + (assetId to token)
                }
            }
        }
    }
    MixinAppTheme {
        Column {
            approvals?.forEachIndexed { index, approval ->
                val token = tokens[approval.assetId]
                AssetChangeItem(
                    status = status,
                    amount = approval.amount,
                    symbol = token?.symbol ?: "",
                    iconUrl = token?.iconUrl,
                    fiatValue = approval.amount.toFiatValue(token, approval.type == "unlimited"),
                    isReceive = false,
                    isUnlimited = approval.type == "unlimited",
                    isApproval = approvals.isEmpty().not()
                )
                if (index < approvals.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            
            if (approvals != null && approvals.isNotEmpty() && (receivers.isNotEmpty() || senders.isNotEmpty())) {
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            receivers.forEachIndexed { index, receiver ->
                val token = tokens[receiver.assetId]
                AssetChangeItem(
                    status = status,
                    amount = receiver.amount,
                    symbol = token?.symbol ?: "",
                    iconUrl = token?.iconUrl,
                    fiatValue = receiver.amount.toFiatValue(token),
                    isReceive = true
                )
                if (index < receivers.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            
            if (receivers.isNotEmpty() && senders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            senders.forEachIndexed { index, sender ->
                val token = tokens[sender.assetId]
                AssetChangeItem(
                    status = status,
                    amount = sender.amount,
                    symbol = token?.symbol ?: "",
                    iconUrl = token?.iconUrl,
                    fiatValue = sender.amount.toFiatValue(token),
                    isReceive = false
                )
                if (index < senders.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

private fun String.toFiatValue(
    token: Web3TokenItem?,
    isUnlimited: Boolean = false,
): String? {
    if (token == null || isUnlimited) return null
    val amountValue = toBigDecimalOrNull() ?: return null
    val fiatValue = amountValue.abs().multiply(token.priceFiat())
    return "${Fiats.getSymbol()}${fiatValue.numberFormat2()}"
}
