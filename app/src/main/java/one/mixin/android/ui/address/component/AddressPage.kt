package one.mixin.android.ui.address.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.address.AddressViewModel
import one.mixin.android.vo.Address
import one.mixin.android.vo.safe.TokenItem

@Composable
fun AddressPage(
    contentText: String,
    token: TokenItem?,
    web3Token: Web3Token?,
    web3Chain: Web3Token?,
) {
    val viewModel: AddressViewModel = hiltViewModel()
    val addresses by viewModel.addressesFlow(token?.assetId?:"").collectAsState(initial = null)
    MixinAppTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            when {
                addresses == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                addresses?.isEmpty() == true -> {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(R.string.Add_address),
                        color = MixinAppTheme.colors.textBlue
                    )
                }

                else -> {
                    AddressList(addresses = addresses!!)
                }
            }
        }
    }
}

@Composable
private fun AddressList(addresses: List<Address>) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(addresses) { address ->
            AddressItem(address = address)
        }
    }
}

@Composable
private fun AddressItem(address: Address) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.backgroundWindow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = address.label.firstOrNull()?.toString() ?: "#",
                color = MaterialTheme.colors.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.W500
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = address.label,
                color = MixinAppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = address.destination + if (!address.tag.isNullOrEmpty()) "\nTag: ${address.tag}" else "",
                color = MixinAppTheme.colors.textAssist,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
