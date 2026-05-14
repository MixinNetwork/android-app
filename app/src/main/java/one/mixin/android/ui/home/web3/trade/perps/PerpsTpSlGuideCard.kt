package one.mixin.android.ui.home.web3.trade.perps

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putLong
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal

internal const val HIDE_TPSL_GUIDE_DURATION_MS = 14L * 24 * 60 * 60 * 1000
internal const val PREF_HIDE_TP_GUIDE_UNTIL = "pref_hide_tp_guide_until"
internal const val PREF_HIDE_SL_GUIDE_UNTIL = "pref_hide_sl_guide_until"

internal enum class TpSlGuideType {
    TAKE_PROFIT,
    STOP_LOSS,
}

internal enum class PerpsTpSlGuideCardLayout {
    DETAIL,
    BOTTOM_SHEET,
}

internal fun getTpSlGuidePreferenceKey(guideType: TpSlGuideType): String {
    return if (guideType == TpSlGuideType.TAKE_PROFIT) {
        PREF_HIDE_TP_GUIDE_UNTIL
    } else {
        PREF_HIDE_SL_GUIDE_UNTIL
    }
}

internal fun SharedPreferences.getTpSlGuideHideUntil(guideType: TpSlGuideType): Long {
    return getLong(getTpSlGuidePreferenceKey(guideType), 0L)
}

internal fun SharedPreferences.hideTpSlGuide(guideType: TpSlGuideType): Long {
    val until = System.currentTimeMillis() + HIDE_TPSL_GUIDE_DURATION_MS
    putLong(getTpSlGuidePreferenceKey(guideType), until)
    return until
}

internal fun resolveTpSlGuideType(
    pnl: BigDecimal,
    hasTakeProfit: Boolean,
    hasStopLoss: Boolean,
    hideTakeProfitGuideUntil: Long,
    hideStopLossGuideUntil: Long,
    now: Long,
): TpSlGuideType? {
    return when {
        !hasTakeProfit && pnl > BigDecimal.ZERO && now >= hideTakeProfitGuideUntil -> TpSlGuideType.TAKE_PROFIT
        !hasStopLoss && pnl < BigDecimal.ZERO && now >= hideStopLossGuideUntil -> TpSlGuideType.STOP_LOSS
        else -> null
    }
}

@Composable
internal fun PerpsTpSlGuideCard(
    guideType: TpSlGuideType,
    onClose: () -> Unit,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    layout: PerpsTpSlGuideCardLayout = PerpsTpSlGuideCardLayout.DETAIL,
) {
    val context = LocalContext.current
    val quoteColorReversed = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val infoTitleRes = when (layout) {
        PerpsTpSlGuideCardLayout.DETAIL -> if (guideType == TpSlGuideType.TAKE_PROFIT) {
            R.string.perps_tpsl_detail_take_profit_title
        } else {
            R.string.perps_tpsl_detail_stop_loss_title
        }
        PerpsTpSlGuideCardLayout.BOTTOM_SHEET -> if (guideType == TpSlGuideType.TAKE_PROFIT) {
            R.string.take_profit_intro_title
        } else {
            R.string.stop_loss_intro_title
        }
    }
    val infoDescRes = when (layout) {
        PerpsTpSlGuideCardLayout.DETAIL -> if (guideType == TpSlGuideType.TAKE_PROFIT) {
            R.string.perps_tpsl_detail_take_profit_description
        } else {
            R.string.perps_tpsl_detail_stop_loss_description
        }
        PerpsTpSlGuideCardLayout.BOTTOM_SHEET -> if (guideType == TpSlGuideType.TAKE_PROFIT) {
            R.string.take_profit_intro_description
        } else {
            R.string.stop_loss_intro_description
        }
    }
    val infoIconRes = if (guideType == TpSlGuideType.TAKE_PROFIT) {
        if (quoteColorReversed) R.drawable.ic_perps_tpsl_info_tp_reversed else R.drawable.ic_perps_tpsl_info_tp
    } else {
        if (quoteColorReversed) R.drawable.ic_perps_tpsl_info_sl_reversed else R.drawable.ic_perps_tpsl_info_sl
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        when (layout) {
            PerpsTpSlGuideCardLayout.DETAIL -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        painter = painterResource(id = infoIconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(40.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.padding(end = 28.dp)) {
                        Text(
                            text = stringResource(infoTitleRes),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W600,
                            color = MixinAppTheme.colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(infoDescRes),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MixinAppTheme.colors.textMinor,
                        )
                        if (actionText != null && onActionClick != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = actionText,
                                fontSize = 13.sp,
                                color = MixinAppTheme.colors.accent,
                                modifier = Modifier.clickable(onClick = onActionClick),
                            )
                        }
                    }
                }
            }
            PerpsTpSlGuideCardLayout.BOTTOM_SHEET -> {
                Column(modifier = Modifier.padding(end = 72.dp)) {
                    Text(
                        text = stringResource(infoTitleRes),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(infoDescRes),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MixinAppTheme.colors.textMinor,
                    )
                    if (actionText != null && onActionClick != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = actionText,
                            fontSize = 13.sp,
                            color = MixinAppTheme.colors.accent,
                            modifier = Modifier.clickable(onClick = onActionClick),
                        )
                    }
                }

                Icon(
                    painter = painterResource(id = infoIconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp)
                        .size(40.dp),
                )
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_close_grey),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.TopEnd)
                .clickable(onClick = onClose),
        )
    }
}
