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
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
fun AlertTypeSelector(selectedType: AlertType, onTypeSelected: (AlertType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .draw9Patch(context, MixinAppTheme.drawables.bgAlertCard)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {  expanded = true }
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
                Text(text = stringResource(id = selectedType.getStringResId()), color = MixinAppTheme.colors.textPrimary)
            }
            Box {
                Image(
                    painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                    contentDescription = null,
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    AlertType.entries.forEach { alertType ->
                        DropdownMenuItem(onClick = {
                            onTypeSelected(alertType)
                            expanded = false
                        }) {
                            Row(modifier = Modifier.width(200.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    modifier = Modifier.alpha(if (selectedType == alertType) 1f else 0f),
                                    painter = painterResource(id = R.drawable.ic_check_black_24dp),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(id = alertType.getStringResId()))
                            }
                        }
                    }
                }
            }
        }
    }
}
