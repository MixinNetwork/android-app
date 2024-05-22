package one.mixin.android.ui.home.web3.swap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.web3.Tx
import one.mixin.android.api.response.web3.TxState
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun SwapStatePage(
    tx: Tx?,
    viewTx: () -> Unit,
    pop: () -> Unit,
) {
    SwapPageScaffold(
        title = stringResource(id = R.string.Swapping),
        verticalScrollable = true,
        pop = pop,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))
            Text(
                text = tx?.state ?: TxState.NotFound.name,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = MixinAppTheme.colors.textPrimary,
                )
            )
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

        }
    }
}