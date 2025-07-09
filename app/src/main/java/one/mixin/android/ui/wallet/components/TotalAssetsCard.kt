package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormat2
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal

@Composable
fun TotalAssetsCard(
    viewModel: AssetDistributionViewModel = hiltViewModel()
) {
    var combinedDistribution by remember { mutableStateOf<List<AssetDistribution>>(emptyList()) }
    var totalBalance by remember { mutableStateOf(BigDecimal.ZERO) }

    LaunchedEffect(viewModel) {
        combinedDistribution = viewModel.getTokenDistribution()
        totalBalance = viewModel.getTokenTotalBalance()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(stringResource(R.string.Total_Balance), color = MixinAppTheme.colors.textPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = totalBalance.numberFormat2(),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.W400,
                fontFamily = FontFamily(Font(R.font.mixin_font))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("USD", color = MixinAppTheme.colors.textAssist, fontSize = 12.sp)
        }
        Spacer(Modifier.height(17.dp))
        MultiColorProgressBar(combinedDistribution)
        Spacer(Modifier.height(12.dp))
        Distribution(combinedDistribution, destination = null)
    }
}
