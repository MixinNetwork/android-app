package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun HelpBottomSheetContent(
    onContactSupport: () -> Unit,
    onTradingGuide: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MixinAppTheme.colors.background)
            .padding(vertical = 16.dp)
    ) {
        HelpOption(
            title = stringResource(R.string.Contact_Support),
            onClick = onContactSupport
        )

        Spacer(modifier = Modifier.height(1.dp))

        HelpOption(
            title = stringResource(R.string.Trading_Guide),
            onClick = onTradingGuide
        )

        Spacer(modifier = Modifier.height(8.dp))

        HelpOption(
            title = stringResource(R.string.Cancel),
            onClick = onDismiss
        )
    }
}

@Composable
private fun HelpOption(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = MixinAppTheme.colors.textPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}
