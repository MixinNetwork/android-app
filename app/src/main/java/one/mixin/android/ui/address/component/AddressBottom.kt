package one.mixin.android.ui.address.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.address.AddressViewModel
import one.mixin.android.vo.safe.TokenItem

@Composable
fun AddressBottom(token: TokenItem) {
    val context = LocalContext.current
    val viewModel: AddressViewModel = hiltViewModel()
    val addresses by viewModel.addressesFlow(token.assetId).collectAsState(initial = null)
    MixinAppTheme {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_home_setting),
                tint = Color.Unspecified,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .clickable {

                    }

            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${addresses?.size ?: 0} ${context.getString(R.string.Address)}",
                color = MixinAppTheme.colors.textAssist
            )
            Spacer(modifier = Modifier.weight(1f))

            Icon(
                painter = painterResource(R.drawable.ic_add_black_24dp),
                tint = Color.Unspecified,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .clickable {

                    }

            )
        }
    }
}