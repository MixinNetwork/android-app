package one.mixin.android.ui.landing.components

import PageScaffold
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun SetPinLoadingPage(pop: () -> Unit, next: () -> Unit) {
    PageScaffold(
        title = "",
        verticalScrollable = false,
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        },
        pop = pop,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(30.dp),
            color = Color(0xFFE8E5EE),
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            onClick = next,
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
                text = stringResource(R.string.Next),
                color = Color.White
            )
        }
    }
}