package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.perps.PositionHistoryView
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ClosedPositionItem(position: PositionHistoryView) {
    val pnl = try {
        BigDecimal(position.realizedPnl)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    val isProfit = pnl >= BigDecimal.ZERO
    val pnlColor = if (isProfit) Color(0xFF4CAF50) else Color(0xFFF44336)
    val pnlText = "${if (isProfit) "+" else ""}$${String.format("%.2f", pnl)}"

    val entryPrice = try {
        val price = BigDecimal(position.entryPrice)
        String.format("%.4f", price)
    } catch (e: Exception) {
        position.entryPrice
    }

    val closePrice = try {
        val price = BigDecimal(position.closePrice)
        String.format("%.4f", price)
    } catch (e: Exception) {
        position.closePrice
    }

    val closedDate = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val outputFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
        val date = inputFormat.parse(position.closedAt)
        date?.let { outputFormat.format(it) } ?: position.closedAt
    } catch (e: Exception) {
        position.closedAt
    }
    
    val displaySymbol = position.marketSymbol ?: "Unknown"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displaySymbol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MixinAppTheme.colors.textPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = position.side.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (position.side.lowercase() == "long") Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .cardBackground(
                            if (position.side.lowercase() == "long") Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f),
                            Color.Transparent
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${position.leverage}x",
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.Entry_Close_Price, entryPrice, closePrice),
                fontSize = 12.sp,
                color = MixinAppTheme.colors.textAssist,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = closedDate,
                fontSize = 11.sp,
                color = MixinAppTheme.colors.textAssist,
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = pnlText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = pnlColor,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${position.quantity} ${displaySymbol}",
                fontSize = 12.sp,
                color = MixinAppTheme.colors.textAssist,
            )
        }
    }
}
