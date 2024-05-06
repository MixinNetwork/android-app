package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.MixinBottomSheetDialog
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun EmergencyContactTipBottomSheet(
    onDismissRequest: () -> Unit,
    onConfirmed: () -> Unit,
) {
    MixinBottomSheetDialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.height(70.dp))
            Text(
                text = stringResource(id = R.string.Emergency_Contact),
                fontSize = 18.sp,
                color = MixinAppTheme.colors.textPrimary,
            )
            Box(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.setting_emergency_content),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textPrimary,
            )
            Box(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.setting_emergency_warning),
                fontSize = 14.sp,
                color = MixinAppTheme.colors.red,
            )
            Box(modifier = Modifier.height(36.dp))
            Image(
                painter = painterResource(id = MixinAppTheme.drawables.emergencyAvatar),
                contentDescription = null,
            )
            Box(modifier = Modifier.height(36.dp))
            TextButton(
                onClick = { onConfirmed() },
                shape = RoundedCornerShape(20.dp),
                modifier =
                    Modifier
                        .width(116.dp)
                        .height(40.dp),
                colors =
                    ButtonDefaults.textButtonColors(
                        backgroundColor = MixinAppTheme.colors.accent,
                        contentColor = MixinAppTheme.colors.textPrimary,
                    ),
            ) {
                Text(
                    text = stringResource(id = R.string.Got_it),
                    fontSize = 14.sp,
                )
            }
            Box(modifier = Modifier.height(40.dp))
        }
    }
}
