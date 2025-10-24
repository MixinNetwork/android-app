package one.mixin.android.ui.address.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun FetchAddressContent(
    state: FetchAddressState,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onCancel: () -> Unit,
) {
    MixinAppTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(120.dp))
            if (state == FetchAddressState.ERROR) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_order_failed),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_wallet_fetching),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (state != FetchAddressState.ERROR) {
                Text(
                    text = stringResource(R.string.Fetching_address),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MixinAppTheme.colors.textPrimary
                )
            } else {
                Text(
                    text = stringResource(R.string.Fetch_Failed),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MixinAppTheme.colors.red,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (errorMessage.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.fetching_shouldnt_take_long),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            } else {
                Text(
                    text = errorMessage,
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            when (state) {
                FetchAddressState.LOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = MixinAppTheme.colors.backgroundGray
                    )
                }

                FetchAddressState.RETRY -> {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                            .height(48.dp),
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MixinAppTheme.colors.accent,
                            disabledBackgroundColor = MixinAppTheme.colors.backgroundGrayLight,
                            contentColor = Color.White,
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(32.dp),
                        elevation = ButtonDefaults.elevation(
                            pressedElevation = 0.dp,
                            defaultElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp,
                        ),
                    ) {
                        Text(
                            text = stringResource(id = R.string.Retry),
                            color = Color.White
                        )
                    }
                }

                FetchAddressState.ERROR -> {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                            .height(48.dp),
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MixinAppTheme.colors.accent,
                            disabledBackgroundColor = MixinAppTheme.colors.backgroundGrayLight,
                            contentColor = Color.White,
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(32.dp),
                        elevation = ButtonDefaults.elevation(
                            pressedElevation = 0.dp,
                            defaultElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp,
                        ),
                    ) {
                        Text(
                            text = stringResource(id = R.string.Retry),
                            color = Color.White
                        )
                    }
                }

                else -> Spacer(modifier = Modifier.height(70.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}