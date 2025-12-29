package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.event.WalletRefreshedEvent
import one.mixin.android.extension.numberFormat2
import one.mixin.android.ui.components.Tooltip
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.WalletCategory
import java.math.BigDecimal

@Composable
fun TotalAssetsCard(
    viewModel: AssetDistributionViewModel = hiltViewModel(),
    selectedCategory: String? = null
) {
    var combinedDistribution by remember { mutableStateOf<List<AssetDistribution>>(emptyList()) }
    var totalBalance by remember { mutableStateOf(BigDecimal.ZERO) }
    var showTooltip by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTrigger by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val disposable = RxBus.listen(WalletRefreshedEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { _ ->
                refreshTrigger++
            }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            disposable.dispose()
        }
    }

    LaunchedEffect(refreshTrigger, selectedCategory) {
        combinedDistribution = viewModel.getTokenDistribution(false, selectedCategory)
        totalBalance = viewModel.getTokenTotalBalance(false, selectedCategory)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.Total_Balance), color = MixinAppTheme.colors.textPrimary)
            Spacer(modifier = Modifier.width(4.dp))
            if (selectedCategory == null || selectedCategory == WalletCategory.MIXIN_SAFE.value) {
                Box {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tip),
                        contentDescription = null,
                        modifier = Modifier
                            .size(12.dp)
                            .clickable { showTooltip = true },
                        tint = MixinAppTheme.colors.textAssist
                    )
                    if (showTooltip) {
                        val xOffset = with(LocalDensity.current) {
                            (-24).dp.toPx()
                        }.toInt()
                        Tooltip(
                            text = stringResource(
                                id =
                                    if (selectedCategory == WalletCategory.MIXIN_SAFE.value) R.string.wallet_summary_tip_safe
                                    else R.string.wallet_summary_tip_all
                            ),
                            onDismissRequest = { showTooltip = false },
                            offset = IntOffset(xOffset, with(LocalDensity.current) {
                                (44).dp.toPx()
                            }.toInt()),
                            arrowOffsetX = 24.dp,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = totalBalance.multiply(Fiats.getRate().toBigDecimal()).numberFormat2(),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.W400,
                fontFamily = FontFamily(Font(R.font.mixin_font))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(Fiats.getAccountCurrencyAppearance(), color = MixinAppTheme.colors.textAssist, fontSize = 12.sp)
        }
        Spacer(Modifier.height(17.dp))
        MultiColorProgressBar(combinedDistribution)
        Spacer(Modifier.height(12.dp))
        Distribution(combinedDistribution, destination = null)
    }
}
