package one.mixin.android.ui.home.web3.components

import GlideImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.sessionrequest.ActionBottom
import one.mixin.android.vo.priceUSD
import one.mixin.android.vo.safe.Token
import java.math.BigDecimal

@Composable
fun TransactionPreview(
    balance: BigDecimal,
    chain: Chain,
    asset: Token?,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MixinAppTheme.colors.background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.Balance_Change),
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
        )
        Box(modifier = Modifier.height(8.dp))
        Row(
            modifier =
            Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                modifier = Modifier.alignByBaseline(),
                text = "-$balance",
                color = Color(0xFFE86B67),
                fontFamily = FontFamily(Font(R.font.mixin_font)),
                fontSize = 30.sp,
            )
            Box(modifier = Modifier.width(4.dp))
            Text(
                modifier = Modifier.alignByBaseline(),
                text = chain.symbol,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 12.sp,
            )
            Box(modifier = Modifier.weight(1f))
            GlideImage(
                data = asset?.iconUrl ?: "",
                modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
            )
        }
        Text(
            text = "≈ $${balance.multiply(asset.priceUSD()).toPlainString()}",
            color = MixinAppTheme.colors.textMinor,
            fontSize = 12.sp,
        )
        Box(modifier = Modifier.height(10.dp))
    }
}


@Composable
fun MessagePreview(
    content: String,
    onPreviewMessage: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(id = R.string.Message),
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
        )
        Box(modifier = Modifier.height(4.dp))
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(0.dp, 128.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MixinAppTheme.colors.backgroundWindow)
                .clickable { onPreviewMessage(content) },
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                text = content,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
            )
            Image(
                painter = painterResource(R.drawable.ic_post),
                modifier =
                Modifier
                    .size(40.dp, 40.dp)
                    .padding(horizontal = 8.dp)
                    .align(Alignment.TopEnd),
                contentDescription = null,
            )
        }
    }
}

@Composable
fun Warning(
    modifier: Modifier,
) {
    var isVisible by remember { mutableStateOf(true) }
    if (isVisible) {
        Row(
            modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(MixinAppTheme.colors.tipWarning)
                .border(1.dp, MixinAppTheme.colors.tipWarningBorder, shape = RoundedCornerShape(8.dp))
                .padding(20.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_warning),
                modifier =
                Modifier
                    .size(40.dp, 40.dp)
                    .padding(horizontal = 7.dp),
                contentDescription = null,
            )
            Box(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = stringResource(id = R.string.decode_transaction_failed),
                    color = MixinAppTheme.colors.tipError,
                    fontSize = 14.sp,
                )
                Box(modifier = Modifier.width(8.dp))
                Row(modifier = Modifier.align(Alignment.End)) {
                    Text(
                        modifier =
                        Modifier.clickable {
                            isVisible = false
                        },
                        text = stringResource(id = R.string.Got_it),
                        color = MixinAppTheme.colors.textBlue,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewMessage() {
    Box(modifier = Modifier.background(MixinAppTheme.colors.background)) {
        MessagePreview(
            content = """{
          "raw": [
            "0x9df67f5a05fb594c4357d87221cbd69f1d5a6fbb",
            "{\"types\":{\"Alias\":[{\"name\":\"from\",\"type\":\"address\"},{\"name\":\"alias\",\"type\":\"address\"},{\"name\":\"timestamp\",\"type\":\"uint64\"}],\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"}]},\"domain\":{\"name\":\"snapshot\",\"version\":\"0.1.4\"},\"primaryType\":\"Alias\",\"message\":{\"from\":\"0x9df67f5a05fb594c4357d87221cbd69f1d5a6fbb\",\"alias\":\"0x8f14e8dbc7b3619e5210201022f637f271545c90\",\"timestamp\":\"1710766295\"}}"
          ],
          "type": "TYPED_MESSAGE"
        }
    """,
        ) {
        }
    }
}

@Preview
@Composable
private fun TransactionPreview() {
    TransactionPreview(balance = BigDecimal(0.134), chain = Chain.Ethereum, null)
}


@Preview
@Composable
private fun WarningPreview() {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        ActionBottom(
            modifier = Modifier.align(Alignment.BottomCenter),
            cancelTitle = stringResource(id = R.string.Cancel),
            confirmTitle = stringResource(id = R.string.Confirm),
            cancelAction = { },
        ) {
        }

        Warning(modifier = Modifier.align(Alignment.BottomCenter))
    }
}