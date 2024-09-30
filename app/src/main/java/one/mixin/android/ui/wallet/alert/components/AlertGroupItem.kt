package one.mixin.android.ui.wallet.alert.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.alert.AlertViewModel
import one.mixin.android.ui.wallet.alert.draw9Patch
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertAction
import one.mixin.android.ui.wallet.alert.vo.AlertGroup
import java.math.BigDecimal

@Composable
fun AlertGroupItem(alertGroup: AlertGroup, initiallyExpanded: Boolean, onEdit: (Alert) -> Unit) {
    var expand by remember { mutableStateOf(initiallyExpanded) }
    val viewModel = hiltViewModel<AlertViewModel>()
    val alerts by viewModel.alertsByCoinId(alertGroup.coinId).collectAsState(initial = emptyList())
    val rotationState by animateFloatAsState(targetValue = if (expand) 0f else -180f, label = "")
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .draw9Patch(context, MixinAppTheme.drawables.bgAlertCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 23.dp)
                .padding(top = 19.dp, bottom = 22.dp)
        ) {
            ConstraintLayout(modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expand = !expand }) {
                val (starIcon, endIcon, title, subtitle) = createRefs()
                CoilImage(
                    alertGroup.iconUrl,
                    modifier = Modifier
                        .constrainAs(starIcon) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                        }
                        .size(42.dp)
                        .clip(CircleShape),
                    placeholder = R.drawable.ic_avatar_place_holder,
                )

                Text(modifier = Modifier.constrainAs(title) {
                    top.linkTo(starIcon.top)
                    start.linkTo(starIcon.end, 10.dp)
                }, text = alertGroup.name, fontSize = 16.sp, lineHeight = 16.sp, color = MixinAppTheme.colors.textPrimary)

                Text(
                    modifier = Modifier.constrainAs(subtitle) {
                        start.linkTo(starIcon.end, 10.dp)
                        bottom.linkTo(starIcon.bottom)
                    }, text = stringResource(R.string.Current_price, "${BigDecimal(alertGroup.priceUsd).priceFormat()} USD"),
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                    color = MixinAppTheme.colors.textAssist
                )
                Icon(
                    modifier = Modifier
                        .graphicsLayer(rotationZ = rotationState)
                        .constrainAs(endIcon) {
                            top.linkTo(starIcon.top)
                            end.linkTo(parent.end)
                            bottom.linkTo(starIcon.bottom)
                        }
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
}

