package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.dp as px
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.alert.components.cardBackground

@AndroidEntryPoint
class WalletBuyOptionsBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "WalletBuyOptionsBottomSheetDialogFragment"
        private const val ARGS_WALLET_NAME = "args_wallet_name"
        private const val ARGS_WALLET_ICON_RES = "args_wallet_icon_res"

        fun newInstance(
            walletName: String,
            @DrawableRes walletIconRes: Int = 0,
        ) = WalletBuyOptionsBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_WALLET_NAME, walletName)
                putInt(ARGS_WALLET_ICON_RES, walletIconRes)
            }
        }
    }

    private var onGooglePayOrCard: (() -> Unit)? = null
    private var onBankTransfer: (() -> Unit)? = null

    fun setOnGooglePayOrCard(callback: () -> Unit): WalletBuyOptionsBottomSheetDialogFragment {
        onGooglePayOrCard = callback
        return this
    }

    fun setOnBankTransfer(callback: () -> Unit): WalletBuyOptionsBottomSheetDialogFragment {
        onBankTransfer = callback
        return this
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            WalletBuyOptionsSheet(
                walletName = requireArguments().getString(ARGS_WALLET_NAME).orEmpty(),
                walletIconRes = requireArguments().getInt(ARGS_WALLET_ICON_RES),
                onClose = { dismiss() },
                onGooglePayOrCard = {
                    dismiss()
                    onGooglePayOrCard?.invoke()
                },
                onBankTransfer = {
                    dismiss()
                    onBankTransfer?.invoke()
                },
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int = 400.px

    override fun showError(error: String) = Unit
}

@Composable
private fun WalletBuyOptionsSheet(
    walletName: String,
    @DrawableRes walletIconRes: Int,
    onClose: () -> Unit,
    onGooglePayOrCard: () -> Unit,
    onBankTransfer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MixinAppTheme.colors.backgroundWindow)
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.Buy),
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.W600,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = walletName,
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    if (walletIconRes != 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(walletIconRes),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MixinAppTheme.colors.backgroundGrayLight)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_wallet_close),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        WalletBuyOptionItem(
            iconRes = R.drawable.ic_wallet_buy_card,
            title = stringResource(R.string.wallet_buy_option_google_pay_or_card),
            description = stringResource(R.string.wallet_buy_option_google_pay_or_card_desc),
            onClick = onGooglePayOrCard,
        )
        Spacer(modifier = Modifier.height(12.dp))
        WalletBuyOptionItem(
            iconRes = R.drawable.ic_wallet_buy_bank_transfer,
            title = stringResource(R.string.wallet_buy_option_bank_transfer),
            description = stringResource(R.string.wallet_buy_option_bank_transfer_desc),
            showBadge = true,
            onClick = onBankTransfer,
        )
    }
}

@Composable
private fun WalletBuyOptionItem(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    showBadge: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp)
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = MixinAppTheme.colors.textMinor,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.W400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(MixinAppTheme.colors.green)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.wallet_buy_option_new),
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.W500,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}
