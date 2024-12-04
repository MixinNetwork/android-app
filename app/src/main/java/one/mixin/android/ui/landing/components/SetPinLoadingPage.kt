package one.mixin.android.ui.landing.components

import PageScaffold
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.landing.MobileViewModel
import one.mixin.android.ui.landing.vo.SetupState

@Composable
fun SetPinLoadingPage(next: () -> Unit) {
    val viewModel = hiltViewModel<MobileViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val setupState by viewModel.setupState.observeAsState(SetupState.Loading)
    val context = LocalContext.current
    PageScaffold(
        title = "",
        verticalScrollable = false,
        actions = {
            IconButton(onClick = {
                context.openUrl(Constants.HelpLink.TIP)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        },
        pop = null,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 36.dp)) {
            Spacer(modifier = Modifier.height(120.dp))
            Icon(painter = painterResource(R.drawable.ic_wallet_pin), tint = Color.Unspecified, contentDescription = null)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(
                    R.string.Set_up_Pin
                ),
                fontSize = 18.sp, fontWeight = FontWeight.W600, color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(
                    R.string.Set_up_pin_warning
                ),
                color = MixinAppTheme.colors.textAssist
            )

            Spacer(modifier = Modifier.weight(1f))

            if (setupState == SetupState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    color = Color(0xFFE8E5EE),
                )
                Text(
                    text = stringResource(R.string.Trying_connect_tip_node),
                    color = MixinAppTheme.colors.textAssist
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else if (setupState == SetupState.Failure) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setState(SetupState.Success)
                            next()
                        }
                    },
                    colors =
                    ButtonDefaults.outlinedButtonColors(
                        backgroundColor = MixinAppTheme.colors.accent
                    ),
                    shape = RoundedCornerShape(32.dp),
                    elevation =
                    ButtonDefaults.elevation(
                        pressedElevation = 0.dp,
                        defaultElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.Retry),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}