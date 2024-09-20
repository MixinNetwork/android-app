package one.mixin.android.ui.wallet.alert.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.vo.AlertFrequency

@Composable
fun AlertFrequencySelector(selectedType: AlertFrequency, onTypeSelected: (AlertFrequency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            backgroundColor = MixinAppTheme.colors.background
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = stringResource(R.string.Alert_Frequency), fontSize = 12.sp, color = MixinAppTheme.colors.textAssist)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = stringResource(id = selectedType.getStringResId()), color = MixinAppTheme.colors.textPrimary)
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AlertFrequency.entries.forEach { alertType ->
                DropdownMenuItem(onClick = {
                    onTypeSelected(alertType)
                    expanded = false
                }) {
                    Text(text = stringResource(id = alertType.getStringResId()))
                }
            }
        }
    }
}