package one.mixin.android.ui.wallet.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import one.mixin.android.R

@Composable
fun ProductScreen(
    viewModel: CheckoutViewModel,
    googlePayButtonOnClick: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val padding = 20.dp
    val black = Color(0xff000000.toInt())
    val grey = Color(0xffeeeeee.toInt())

    if (state.checkoutSuccess) {
        Column(
            modifier = Modifier
                .testTag("successScreen")
                .background(grey)
                .padding(padding)
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                contentDescription = null,
                painter = painterResource(R.drawable.check_circle),
                modifier = Modifier
                    .width(200.dp)
                    .height(200.dp),
            )
            Text(text = "The payment completed successfully.\nWe are preparing your order.")
        }
    } else {
        Column(
            modifier = Modifier
                .background(grey)
                .padding(padding)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(space = padding / 2),
        ) {
            if (state.googlePayAvailable == true) {
                Button(onClick = { if (state.googlePayButtonClickable) googlePayButtonOnClick() }) {
                    Text("Pay")
                }
            } else {
                Text("Pay No Available")
            }
        }
    }
}
