package one.mixin.android.ui.home.web3.components


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat8
import one.mixin.android.ui.home.web3.trade.InputContent
import one.mixin.android.ui.home.web3.trade.SwapViewModel
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal

@Composable
fun InputArea(
    modifier: Modifier = Modifier,
    token: SwapToken?,
    text: String,
    title: String,
    readOnly: Boolean,
    selectClick: (() -> Unit)?,
    onInputChanged: ((String) -> Unit)? = null,
    onDeposit: ((SwapToken) -> Unit)? = null,
    onMax: (() -> Unit)? = null,
    bottomCompose: (@Composable () -> Unit)? = null,
    inlineEndCompose: (@Composable () -> Unit)? = null,
) {
    val viewModel = hiltViewModel<SwapViewModel>()
    val balance = if (token == null) {
        null
    } else {
        viewModel.tokenExtraFlow(token).collectAsStateWithLifecycle(token.balance).value
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(8.dp))
                .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor)
                .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 14.sp, color = MixinAppTheme.colors.textPrimary)
                Spacer(modifier = Modifier.weight(1f))
                token?.let {
                    Text(text = it.chain.name, fontSize = 12.sp, color = MixinAppTheme.colors.textAssist)
                } ?: run {
                    Text(text = stringResource(id = R.string.select_token), fontSize = 14.sp, color = MixinAppTheme.colors.textMinor)
                }
            }
        }
        Box(modifier = Modifier.height(10.dp))
        InputContent(token = token, text = text, selectClick = selectClick, onInputChanged = onInputChanged, readOnly = readOnly, inlineEndCompose = inlineEndCompose)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            token?.let { t ->
                val depositVisible = !readOnly && onDeposit != null && (balance?.toBigDecimalOrNull()?.compareTo(BigDecimal.ZERO) ?: 0) == 0
                if (bottomCompose != null) {
                    bottomCompose.invoke()
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_web3_wallet),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.textAssist,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (token.isWeb3) {
                            balance?.numberFormat() ?: "0"
                        } else {
                            balance?.numberFormat8() ?: "0"
                        },
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = MixinAppTheme.colors.textAssist,
                            textAlign = TextAlign.Start,
                        ),
                        modifier = Modifier.clickable { onMax?.invoke() }
                    )
                }
                if (depositVisible) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.Deposit),
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = MixinAppTheme.colors.textBlue,
                        ),
                        modifier = Modifier.clickable { onDeposit.invoke(t) },
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = token.name,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist,
                        textAlign = TextAlign.Start,
                    )
                )
            } ?: run {
                Text(
                    text = "0",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.textAssist,
                        textAlign = TextAlign.Start,
                    ),
                )
            }

        }
    }
}