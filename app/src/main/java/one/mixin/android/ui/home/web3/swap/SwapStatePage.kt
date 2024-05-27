package one.mixin.android.ui.home.web3.swap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.web3.QuoteResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Tx
import one.mixin.android.api.response.web3.isFinalTxState
import one.mixin.android.api.response.web3.isTxFailed
import one.mixin.android.api.response.web3.isTxSuccess
import one.mixin.android.compose.GlideImage
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun SwapStatePage(
    tx: Tx,
    toToken: SwapToken,
    viewTx: () -> Unit,
    close: () -> Unit,
) {
    Scaffold(
        backgroundColor = MixinAppTheme.colors.background,
        topBar = {},
    ) {
        Column(
            Modifier
                .padding(it)
                .apply {
                    verticalScroll(rememberScrollState())
                },
        ) {
            Content(tx, toToken, viewTx, close)
        }
    }
}

@Composable
private fun Content(
    tx: Tx,
    toToken: SwapToken,
    viewTx: () -> Unit,
    close: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Spacer(modifier = Modifier.height(100.dp))
        StateInfo(tx = tx, toToken)
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier
            .clickable {
                viewTx.invoke()
            }) {
            Text(
                text = stringResource(id = R.string.View_Transaction),
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = MixinAppTheme.colors.accent,
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            onClick = {
                close.invoke()
            },
            colors =
            ButtonDefaults.outlinedButtonColors(
                backgroundColor = MixinAppTheme.colors.accent,
            ),
            shape = RoundedCornerShape(32.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            elevation =
            ButtonDefaults.elevation(
                pressedElevation = 0.dp,
                defaultElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp,
            ),
        ) {
            Text(text = stringResource(id = R.string.Close), color = Color.White)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun StateInfo(tx: Tx, toToken: SwapToken) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (tx.state.isFinalTxState()) {
            Icon(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = if (tx.state.isTxSuccess()) R.drawable.ic_order_success else R.drawable.ic_order_failed),
                contentDescription = null,
                tint =  if (tx.state.isTxSuccess()) Color(0xFF5DBC7A) else Color(0xFFF4AB2D)
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            CircularProgressIndicator(
                modifier =
                Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally),
                color = MixinAppTheme.colors.accent,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = if (tx.state.isTxSuccess()) {
              R.string.Transaction_Success
            } else if (tx.state.isTxFailed()) {
                R.string.Transaction_Failed
            } else {
                R.string.Confirming_Transaction
            }),
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.W600,
                color = MixinAppTheme.colors.textPrimary,
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            modifier = Modifier.alpha(if (tx.state.isTxFailed()) 0f else 1f),
            text = stringResource(id = R.string.swap_desc, toToken.symbol),
            style = TextStyle(
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textSubtitle,
            )
        )
    }
}
