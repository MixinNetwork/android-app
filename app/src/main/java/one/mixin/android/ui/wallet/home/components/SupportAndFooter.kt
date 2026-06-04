package one.mixin.android.ui.wallet.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.home.WalletHomeCallbacks
import one.mixin.android.ui.wallet.home.WalletHomeType

@Composable
internal fun SupportCard(callbacks: WalletHomeCallbacks) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.wallet_home_support),
            color = MixinAppTheme.colors.textMinor,
            fontSize = 14.sp,
            fontWeight = FontWeight.W400,
        )
        Spacer(modifier = Modifier.height(20.dp))
        SupportRow(
            iconRes = R.drawable.ic_help_outline,
            titleRes = R.string.wallet_home_contact_us,
            subtitleRes = R.string.wallet_home_contact_us_desc,
            trailingRes = R.drawable.ic_arrow_gray_right,
            onClick = callbacks::onSupportClicked,
        )
        Spacer(modifier = Modifier.height(20.dp))
        SupportRow(
            iconRes = R.drawable.ic_bot_category_books,
            titleRes = R.string.wallet_home_help_center,
            subtitleRes = R.string.wallet_home_help_center_desc,
            trailingRes = R.drawable.ic_wallet_home_external_link,
            onClick = callbacks::onHelpCenterClicked,
        )
    }
}

@Composable
private fun SupportRow(
    iconRes: Int,
    titleRes: Int,
    subtitleRes: Int,
    trailingRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.backgroundWindow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(subtitleRes),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            )
        }
        Icon(
            painter = painterResource(trailingRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun ImportSafetyFooter(walletType: WalletHomeType) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
    ) {
        val titleRes: Int
        val bulletRes: List<Int>
        if (walletType == WalletHomeType.PRIVACY) {
            titleRes = R.string.wallet_home_privacy_wallet_reason_title
            bulletRes = listOf(
                R.string.wallet_home_privacy_wallet_reason_1,
                R.string.wallet_home_privacy_wallet_reason_2,
                R.string.wallet_home_privacy_wallet_reason_3,
            )
        } else {
            titleRes = R.string.wallet_home_common_wallet_reason_title
            bulletRes = listOf(
                R.string.wallet_home_common_wallet_reason_1,
                R.string.wallet_home_common_wallet_reason_2,
                R.string.wallet_home_common_wallet_reason_3,
            )
        }
        ImportSafetySection(titleRes = titleRes, bulletRes = bulletRes)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val previews = listOf(
                R.drawable.mixin_import_safety_preview_0,
                R.drawable.mixin_import_safety_preview_1,
                R.drawable.mixin_import_safety_preview_2,
                R.drawable.mixin_import_safety_preview_3,
                R.drawable.mixin_import_safety_preview_4,
                R.drawable.mixin_import_safety_preview_5,
                R.drawable.mixin_import_safety_preview_6,
            )
            previews.forEachIndexed { index, res ->
                if (index != 0) Spacer(modifier = Modifier.width(10.dp))
                Image(
                    painter = painterResource(id = res),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

@Composable
private fun ImportSafetySection(
    titleRes: Int,
    bulletRes: List<Int>,
) {
    Text(
        text = stringResource(titleRes),
        color = MixinAppTheme.colors.textAssist,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    )
    bulletRes.forEach { res ->
        ImportSafetyBullet(res, topPadding = 8.dp)
    }
}

@Composable
private fun ImportSafetyBullet(textRes: Int, topPadding: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.textAssist),
        )
        Text(
            text = stringResource(textRes),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        )
    }
}
