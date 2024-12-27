package one.mixin.android.ui.wallet.alert.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.draw9Patch
import one.mixin.android.ui.wallet.alert.vo.AlertFrequency

@Composable
fun AlertFrequencySelector(selectedFrequency: AlertFrequency, onFrequencyClick: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onFrequencyClick.invoke() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 23.dp)
                .padding(top = 19.dp, bottom = 22.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = stringResource(R.string.Alert_Frequency), fontSize = 12.sp, color = MixinAppTheme.colors.textAssist)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = stringResource(id = selectedFrequency.getTitleResId()), color = MixinAppTheme.colors.textPrimary)
            }
            Image(
                painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}