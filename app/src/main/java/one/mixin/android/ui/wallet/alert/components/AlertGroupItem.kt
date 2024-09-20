package one.mixin.android.ui.wallet.alert.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Card
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.alert.AlertViewModel
import one.mixin.android.ui.wallet.alert.vo.AlertGroup
import java.math.BigDecimal
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun AlertGroupItem(alertGroup: AlertGroup, initiallyExpanded: Boolean) {
    var expand by remember { mutableStateOf(initiallyExpanded) }
    val viewModel = hiltViewModel<AlertViewModel>()
    val alerts by viewModel.alertsByAssetId(alertGroup.assetId).collectAsState(initial = emptyList())
    val rotationState by animateFloatAsState(targetValue = if (expand) -180f else 0f, label = "")

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MixinAppTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expand = !expand },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    CoilImage(
                        alertGroup.iconUrl,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        placeholder = R.drawable.ic_avatar_place_holder,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(alertGroup.name, fontSize = 16.sp, color = MixinAppTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.Current_price, "${BigDecimal(alertGroup.priceUsd).priceFormat()} USD"),
                            fontSize = 13.sp,
                            color = MixinAppTheme.colors.textAssist
                        )
                    }
                }
                Icon(
                    modifier = Modifier
                        .wrapContentSize()
                        .graphicsLayer(rotationZ = rotationState),
                    painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }

            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth(), visible = expand,
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
                        AlertItem(alert)
                    }
                }
            }
        }
    }
}

