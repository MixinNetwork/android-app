/*
 * Copyright 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package one.mixin.android.ui.wallet.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R

@Composable
fun ProductScreen(
    title: String,
    description: String,
    price: String,
    image: Int,
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
            Image(
                contentDescription = null,
                painter = painterResource(image),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
            )
            Text(
                text = title,
                color = black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(text = price, color = black)
            Spacer(Modifier)
            Text(
                text = "Description",
                color = black,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                color = black,
            )
            if (state.googlePayAvailable == true) {
                Button(onClick = { if (state.googlePayButtonClickable) googlePayButtonOnClick() }) {
                    Text("payButton")
                }
                // PayButton(
                //     modifier = Modifier
                //         .testTag("payButton")
                //         .fillMaxWidth(),
                //     onClick = { if (state.googlePayButtonClickable) googlePayButtonOnClick() },
                //     allowedPaymentMethods = PaymentsUtil.allowedPaymentMethods.toString()
                // )
            }
        }
    }
}
