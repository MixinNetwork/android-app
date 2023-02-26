package one.mixin.android.ui.tip.wc.sessionrequest

import GlideImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import one.mixin.android.R
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.connections.Loading
import one.mixin.android.ui.tip.wc.sessionproposal.DAppInfo
import one.mixin.android.ui.tip.wc.sessionproposal.WCPinBoard
import org.web3j.utils.Convert
import org.web3j.utils.Numeric

@Composable
fun SessionRequestPage(
    version: WalletConnect.Version,
    step: WalletConnectBottomSheetDialogFragment.Step,
    fee: String?,
    errorInfo: String?,
    onDismissRequest: () -> Unit,
    onBiometricClick: () -> Unit,
    onPinComplete: (String) -> Unit,
) {
    val viewModel = hiltViewModel<SessionRequestViewModel>()
    val sessionRequestUI = viewModel.getSessionRequestUI(version)
    if (sessionRequestUI == null) {
        Loading()
        return
    }

    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .fillMaxWidth()
            .background(MixinAppTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_close_black_24dp),
            modifier = Modifier
                .size(40.dp, 40.dp)
                .align(alignment = Alignment.End)
                .padding(horizontal = 8.dp)
                .clip(CircleShape)
                .clickable(onClick = {
                    viewModel.rejectRequest(version, sessionRequestUI.requestId)
                    onDismissRequest.invoke()
                }),
            contentDescription = null,
        )
        Box(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.Signature_Request),
            style = TextStyle(
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.W600,
            ),
        )
        Box(modifier = Modifier.height(8.dp))
        DAppInfo(
            info = sessionRequestUI.peerUI.name,
            icon = sessionRequestUI.peerUI.icon,
        )
        Box(modifier = Modifier.height(16.dp))
        if (sessionRequestUI.data is WCEthereumSignMessage) {
            Message(content = viewModel.getContent(version, sessionRequestUI.data)) {
            }
        } else if (sessionRequestUI.data is WCEthereumTransaction) {
            Transaction(balance = Convert.fromWei(Numeric.toBigInt(sessionRequestUI.data.value).toBigDecimal(), Convert.Unit.ETHER).toPlainString() ?: "0")
        }
        NetworkInfo(name = sessionRequestUI.chain ?: "", fee = fee ?: "0")
        Box(modifier = Modifier.width(16.dp))
        Warning()
        Box(modifier = Modifier.width(20.dp))
        WCPinBoard(
            step = step,
            errorInfo = errorInfo,
            allowBiometric = true,
            onCancelClick = { },
            onApproveClick = { },
            onBiometricClick = { onBiometricClick.invoke() },
            onPinComplete = { pin -> onPinComplete.invoke(pin) },
        )
    }
}

@Composable
private fun Transaction(
    balance: String,
    icon: String = "https://mixin-images.zeromesh.net/zVDjOxNTQvVsA8h2B4ZVxuHoCF3DJszufYKWpd9duXUSbSapoZadC7_13cnWBqg0EmwmRcKGbJaUpA8wFfpgZA=s128",
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MixinAppTheme.colors.backgroundWindow)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(id = R.string.Balance_Change),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, bottom = 2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "-$balance",
                color = Color(0xFFE86B67),
                fontFamily = FontFamily(Font(R.font.mixin_font)),
                fontSize = 30.sp,
            )
            Box(modifier = Modifier.width(4.dp))
            Text(
                text = "ETH",
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 12.sp,
            )
            Box(modifier = Modifier.weight(1f))
            GlideImage(
                data = icon,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
            )
        }
        Text(
            modifier = Modifier.padding(top = 76.dp),
            text = "≈ $100",
            color = MixinAppTheme.colors.textMinor,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun Message(
    content: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MixinAppTheme.colors.backgroundWindow)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            modifier = Modifier.padding(top = 12.dp),
            text = stringResource(id = R.string.Message),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
        )
        Text(
            modifier = Modifier.padding(top = 38.dp, bottom = 8.dp),
            text = content,
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 12.sp,
        )
        Image(
            painter = painterResource(R.drawable.ic_post),
            modifier = Modifier
                .size(40.dp, 40.dp)
                .align(Alignment.TopEnd)
                .padding(horizontal = 8.dp),
            contentDescription = null,
        )
    }
}

@Composable
private fun NetworkInfo(
    name: String,
    fee: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.Network),
                color = MixinAppTheme.colors.textSubtitle,
                fontSize = 14.sp,
            )
            Text(
                text = name,
                color = MixinAppTheme.colors.textSubtitle,
                fontSize = 14.sp,
            )
        }
        Box(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.Network_fee),
                color = MixinAppTheme.colors.textSubtitle,
                fontSize = 14.sp,
            )
            Text(
                text = fee,
                color = MixinAppTheme.colors.textSubtitle,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun Warning() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF7AD))
            .alpha(0.7f)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_warning),
            modifier = Modifier
                .size(40.dp, 40.dp)
                .padding(horizontal = 8.dp),
            contentDescription = null,
        )
        Box(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier.padding(vertical = 12.dp),
            text = stringResource(id = R.string.signature_request_warning),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
        )
        Box(modifier = Modifier.width(16.dp))
    }
}

@Preview
@Composable
private fun TransactionPreview() {
    Transaction(balance = "0.134")
}
