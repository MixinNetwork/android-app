package one.mixin.android.ui.wallet.alert.components

import androidx.compose.foundation.Image
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.draw9Patch
import one.mixin.android.ui.wallet.alert.vo.AlertType

@Composable
fun AlertTypeSelector(selectedType: AlertType, onTypeClick: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .draw9Patch(context, MixinAppTheme.drawables.bgAlertCard)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onTypeClick.invoke()
            }
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
                Text(text = stringResource(R.string.Alert_Type), fontSize = 12.sp, color = MixinAppTheme.colors.textAssist)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = stringResource(id = selectedType.getTitleResId()), color = MixinAppTheme.colors.textPrimary)
            }
            Image(
                painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                contentDescription = null,
            )
        }
    }
}
