package one.mixin.android.ui.home.web3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme

@Composable
fun BrowserPage(showPin: () -> Unit) {
    Box(modifier = Modifier.height(100.dp),contentAlignment = Alignment.Center) {
        Button(
            onClick = showPin,
            colors =
            ButtonDefaults.outlinedButtonColors(
                backgroundColor = MixinAppTheme.colors.accent,
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 11.dp),
        ) {
            Text(text = stringResource(id = R.string.Done), color = Color.White)
        }
    }
}