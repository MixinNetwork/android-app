package one.mixin.android.ui.tip.wc

import GlideImage
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustwallet.walletconnect.models.WCPeerMeta
import one.mixin.android.R
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme

@Composable
fun WalletConnectCompose(
    step: WCStep,
    networkName: String?,
    peerMeta: WCPeerMeta?,
    action: String,
    desc: String?,
    onDismissClick: (() -> Unit),
    onCancelClick: (() -> Unit),
    onApproveClick: (() -> Unit),
    onBiometricClick: (() -> Unit),
    onPinComplete: ((String) -> Unit),
) {
    MixinAppTheme {
        Column(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .fillMaxWidth()
                .height(690.dp)
                .background(MixinAppTheme.colors.background)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_close_black_24dp),
                modifier = Modifier
                    .size(40.dp, 40.dp)
                    .align(alignment = Alignment.End)
                    .padding(horizontal = 8.dp)
                    .clip(CircleShape)
                    .clickable {
                        onDismissClick()
                    },
                contentDescription = null
            )
            if (networkName != null) {
                NetworkInfo(name = networkName)
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (peerMeta != null) {
                PeerMeta(peerMeta = peerMeta)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier
                    .align(alignment = CenterHorizontally)
                    .padding(horizontal = 32.dp),
                text = action,
                textAlign = TextAlign.Center,
                color = MixinAppTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            if (desc != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier
                        .align(alignment = CenterHorizontally)
                        .padding(horizontal = 32.dp),
                    textAlign = TextAlign.Start,
                    text = desc,
                    color = MixinAppTheme.colors.textMinor
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            WalletConnectPinKeyBoard(
                step,
                onCancelClick = onCancelClick,
                onApproveClick = onApproveClick,
                onBiometricClick = {
                    onBiometricClick()
                }
            ) { pin ->
                onPinComplete.invoke(pin)
            }
        }
    }
}

@Composable
fun PeerMeta(peerMeta: WCPeerMeta) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .align(Center)
        ) {
            GlideImage(
                data = peerMeta.icons.last(),
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                peerMeta.url,
                modifier = Modifier.align(CenterVertically),
                textAlign = TextAlign.Center,
                color = MixinAppTheme.colors.textPrimary
            )
        }
    }
}

@Composable
fun NetworkInfo(name: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp).align(Center),
            verticalAlignment = CenterVertically
        ) {
            Canvas(modifier = Modifier.padding(start = 8.dp, end = 8.dp).size(6.dp)) {
                drawCircle(Color(0xFF3D75E3))
            }
            Text(
                name,
                color = MixinAppTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
@Preview
fun WalletConnectComposePreview() {
    WalletConnectCompose(
        step = WCStep.Input,
        networkName = "Polygon Mainnet",
        peerMeta = WCPeerMeta("MyCrypto", "https://app.mycrypto.com", "test description", listOf("")),
        action = "Sign Transaction",
        desc = "long descccccccccccccccccccccccccccccccccccccccccccccccccccc",
        {}, {}, {}, {}, {}
    )
}
