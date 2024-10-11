package one.mixin.android.ui.wallet.alert.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.vo.AlertFrequency

@Composable
fun AlertFrequencyBottom(
    currentAlertFrequency: AlertFrequency,
    onFrequencySelected: (AlertFrequency) -> Unit,
    onDismissRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 12.dp, topStart = 12.dp))
            .background(color = MixinAppTheme.colors.background)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.Alert_Frequency),
                style =
                TextStyle(
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 18.sp,
                ),
            )
            Image(
                painter = painterResource(R.drawable.ic_circle_close),
                modifier =
                Modifier
                    .padding(horizontal = 8.dp)
                    .clip(CircleShape)
                    .clickable {
                        onDismissRequest()
                    },
                contentDescription = null,
            )
        }


        LazyColumn {
            items(AlertFrequency.entries.toTypedArray()) { alertFrequency ->
                Spacer(Modifier.height(8.dp))

                AlertSelectItem(
                    currentAlertFrequency == alertFrequency,
                    alertFrequency.getIconResId(),
                    alertFrequency.getTitleResId(),
                    alertFrequency.getSubTitleResId(),
                    onClick = {
                        onFrequencySelected.invoke(alertFrequency)
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}
