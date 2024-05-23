package one.mixin.android.ui.home.web3.swap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.web3.QuoteResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.GlideImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.components.ActionBottom

@Composable
fun SwapOrderPage(
    quoteResp: QuoteResponse,
    fromToken: SwapToken,
    toToken: SwapToken,
    cancelAction: () -> Unit,
    confirmAction: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(false) }
    MixinAppTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TokenArea(fromToken, quoteResp.inAmount.toLongOrNull() ?: 0L, title = stringResource(id = R.string.Pay))
            TokenArea(toToken, quoteResp.outAmount.toLongOrNull() ?: 0L, title = stringResource(id = R.string.Received))
            Box {
                if (isLoading) {
                    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                        Box(modifier = Modifier.height(24.dp))
                        CircularProgressIndicator(
                            modifier =
                            Modifier
                                .size(40.dp)
                                .align(Alignment.CenterHorizontally),
                            color = MixinAppTheme.colors.accent,
                        )
                        Box(modifier = Modifier.height(22.dp))
                    }
                } else {
                    ActionBottom(
                        modifier =
                        Modifier
                            .align(Alignment.BottomCenter),
                        cancelTitle = stringResource(R.string.Cancel),
                        confirmTitle = stringResource(id = R.string.Continue),
                        cancelAction = cancelAction,
                        confirmAction = {
                            isLoading = true
                            confirmAction.invoke()

                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun TokenArea(
    token: SwapToken?,
    amount: Long,
    title: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 13.sp, color = MixinAppTheme.colors.textMinor)
                Spacer(modifier = Modifier.width(4.dp))
                GlideImage(
                    data = token?.logoURI ?: "",
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape),
                    placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = token?.name ?: "", fontSize = 13.sp, color = MixinAppTheme.colors.textMinor)
            }
        }
        Text(
            text = "${token?.toStringAmount(amount) ?: "0"} ${token?.symbol}",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.W500,
                color = MixinAppTheme.colors.textPrimary, textAlign = TextAlign.End
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
    }
}