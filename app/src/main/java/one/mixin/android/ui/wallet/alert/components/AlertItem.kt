package one.mixin.android.ui.wallet.alert.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.wallet.alert.AlertViewModel
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertAction
import one.mixin.android.ui.wallet.alert.vo.AlertStatus
import one.mixin.android.ui.wallet.alert.vo.AlertType.PERCENTAGE_DECREASED
import one.mixin.android.ui.wallet.alert.vo.AlertType.PRICE_DECREASED
import one.mixin.android.ui.wallet.alert.vo.AlertType.PRICE_INCREASED
import one.mixin.android.ui.wallet.alert.vo.AlertType.PRICE_REACHED

@Composable
fun AlertItem(alert: Alert, onEdit: (Alert) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val viewModel = hiltViewModel<AlertViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    Box {
        ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
            val (starIcon, endIcon, title, subtitle) = createRefs()

            Icon(
                modifier = Modifier
                    .constrainAs(starIcon) {
                        top.linkTo(endIcon.top)
                        start.linkTo(parent.start)
                        bottom.linkTo(endIcon.bottom)
                    }
                    .wrapContentSize(),
                painter = painterResource(id = alert.type.getIconResId()),
                contentDescription = null,
                tint = alert.getTintColor(quoteColorPref),
            )

            Text(
                "${if (alert.type in listOf(PRICE_REACHED, PRICE_INCREASED, PRICE_DECREASED)) stringResource(R.string.Price) else if(alert.type == PERCENTAGE_DECREASED) stringResource(R.string.alert_type_percentage_decreased) else stringResource(R.string.alert_type_percentage_increased)} ${alert.displayValue}", modifier = Modifier.constrainAs(title) {
                    top.linkTo(endIcon.top)
                    bottom.linkTo(endIcon.bottom)
                    linkTo(starIcon.end, endIcon.start, startMargin = 10.dp, endMargin = 10.dp, bias = 0f)
                }, style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 14.sp
                ), color = if (alert.status == AlertStatus.RUNNING) MixinAppTheme.colors.textPrimary else MixinAppTheme.colors.textAssist
            )

            Box(modifier = Modifier
                .constrainAs(endIcon) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MixinAppTheme.colors.accent,
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                expanded = true
                            },
                        painter = painterResource(id = R.drawable.ic_alert_more),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        AlertAction.entries.forEach { action ->
                            val currentStatus = alert.status
                            if (currentStatus == AlertStatus.RUNNING && action == AlertAction.RESUME) {
                                return@forEach
                            } else if (currentStatus == AlertStatus.PAUSED && action == AlertAction.PAUSE) {
                                return@forEach
                            }
                            DropdownMenuItem(onClick = {
                                expanded = false
                                coroutineScope.launch {
                                    when (action) {
                                        AlertAction.EDIT -> onEdit(alert)
                                        else -> {
                                            loading = true
                                            viewModel.updateAlert(alert.alertId, action)
                                            loading = false
                                        }
                                    }
                                }
                            }) {
                                Row(
                                    modifier = Modifier.width(200.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = stringResource(action.getStringResId()))
                                    Icon(
                                        painter = painterResource(id = action.getIconResId()),
                                        contentDescription = null,
                                        modifier = Modifier.wrapContentSize(),
                                        tint = Color.Unspecified,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                if (alert.status == AlertStatus.RUNNING) {
                    stringResource(alert.frequency.getStringResId())
                } else {
                    stringResource(R.string.Paused)
                }, modifier = Modifier.constrainAs(subtitle) {
                    top.linkTo(title.bottom, margin = 4.dp)
                    start.linkTo(title.start)
                }, fontSize = 12.sp, color = MixinAppTheme.colors.textAssist
            )
        }
    }
}