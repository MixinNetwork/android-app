package one.mixin.android.ui.wallet.alert.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.alert.AlertViewModel
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertGroup
import java.math.BigDecimal

@Composable
fun AlertGroupItem(alertGroup: AlertGroup, initiallyExpanded: Boolean, onEdit: (Alert) -> Unit) {
    var expand by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val viewModel = hiltViewModel<AlertViewModel>()
    val alerts by viewModel.alertsByCoinId(alertGroup.coinId).collectAsState(initial = emptyList())
    val rotationState by animateFloatAsState(targetValue = if (expand) 0f else -180f, label = "")
    LocalContext.current
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expand = !expand },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoilImage(
                        alertGroup.iconUrl,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        placeholder = R.drawable.ic_avatar_place_holder,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = alertGroup.name ?: "",
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                            color = MixinAppTheme.colors.textPrimary,
                        )
                        Text(
                            text = stringResource(R.string.Current_price, "${BigDecimal(alertGroup.priceUsd ?: "0").priceFormat()} USD"),
                            fontSize = 13.sp,
                            lineHeight = 13.sp,
                            color = MixinAppTheme.colors.textAssist,
                        )
                    }
                    Icon(
                        modifier = Modifier
                            .graphicsLayer(rotationZ = rotationState)
                            .wrapContentSize()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expand = !expand },
                        painter = painterResource(id = R.drawable.ic_alert_arrow_down),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                }

                AnimatedVisibility(
                    modifier = Modifier
                        .fillMaxWidth(),
                    visible = expand,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        alerts.forEachIndexed { index, alert ->
                            if (index != 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            AlertItem(alert, onEdit)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

