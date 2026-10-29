package one.mixin.android.ui.setting.member

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.dp as px
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.WalletBuyOptionItem

@AndroidEntryPoint
class MixinMemberRenewBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "MixinMemberRenewBottomSheetDialogFragment"

        fun newInstance() = MixinMemberRenewBottomSheetDialogFragment()
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            MemberRenewOptionsSheet(
                onClose = { dismiss() },
                onCard = {
                    val fragmentManager = parentFragmentManager
                    dismiss()
                    MixinMemberUpgradeBottomSheetDialogFragment.newInstance(Session.getAccount()?.membership?.plan)
                        .showNow(fragmentManager, MixinMemberUpgradeBottomSheetDialogFragment.TAG)
                },
                onCrypto = {
                    val activity = requireActivity()
                    val fragmentManager = parentFragmentManager
                    dismiss()
                    Constants.HelpLink.SAFE_RENEW_PLAN.openAsUrlOrWeb(
                        activity, null, fragmentManager, activity.lifecycleScope,
                    )
                },
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int = 340.px

    override fun showError(error: String) = Unit
}

@Composable
private fun MemberRenewOptionsSheet(
    onClose: () -> Unit,
    onCard: () -> Unit,
    onCrypto: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MixinAppTheme.colors.background)
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.Renew_Plan),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.W600,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_circle_close),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        WalletBuyOptionItem(
            iconRes = R.drawable.ic_wallet_buy_card,
            title = stringResource(R.string.wallet_buy_option_google_pay_or_card),
            description = stringResource(R.string.membership_renew_option_card_desc),
            outlined = true,
            onClick = onCard,
        )
        Spacer(modifier = Modifier.height(10.dp))
        WalletBuyOptionItem(
            iconRes = R.drawable.ic_member_renew_crypto,
            title = stringResource(R.string.Crypto),
            description = stringResource(R.string.membership_renew_option_crypto_desc),
            outlined = true,
            onClick = onCrypto,
        )
        Spacer(modifier = Modifier.height(40.dp))
    }
}
