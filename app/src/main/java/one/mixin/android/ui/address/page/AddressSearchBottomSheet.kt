package one.mixin.android.ui.address.page

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.GetActionBarHeight
import one.mixin.android.compose.GetStatusBarHeightValue
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.address.AddressBookCard
import one.mixin.android.ui.address.AddressBookNetwork
import one.mixin.android.ui.address.component.SearchTextField
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.components.DropdownMenuNoPaddingVeitical
import one.mixin.android.vo.Address
import one.mixin.android.vo.Chain
import one.mixin.android.widget.components.MixinButton

@Composable
fun AddressSearchBottomSheet(
    addresses: List<Address>,
    chainId: String,
    chain: Chain?,
    onAddressClick: (Address) -> Unit,
    onAddClick: () -> Unit,
    onDeleteAddress: (Address) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    val filteredAddresses = remember(searchText, addresses) {
        if (searchText.isBlank()) {
            addresses
        } else {
            addresses.filter { address ->
                address.label.contains(searchText, ignoreCase = true) ||
                    address.destination.contains(searchText, ignoreCase = true)
            }
        }
    }
    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                LocalConfiguration.current.screenHeightDp.dp - GetActionBarHeight() - GetStatusBarHeightValue()
            )
            .background(MixinAppTheme.colors.background)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.weight(1f),
                backgroundColor = MixinAppTheme.colors.backgroundWindow,
                hint = stringResource(R.string.address_book_search_hint),
            )
            Text(
                text = stringResource(R.string.Cancel),
                color = MixinAppTheme.colors.textBlue,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
            )
        }
        Row(
            modifier = Modifier
                .padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor, cornerRadius = 24.dp)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AddressBookNetwork(chainId, chain)
        }
        if (filteredAddresses.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ic_empty_file), contentDescription = null)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(if (searchText.isBlank()) R.string.address_book_no_records else R.string.address_book_no_records_found),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filteredAddresses, key = { it.addressId }) { address ->
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        AddressBookCard(
                            address = address,
                            chain = chain,
                            onClick = { onAddressClick(address) },
                            onLongClick = { expanded = true },
                        )
                        DropdownMenuNoPaddingVeitical(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(
                                color = MixinAppTheme.colors.background,
                                shape = RoundedCornerShape(12.dp),
                            ),
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    onDeleteAddress(address)
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    stringResource(R.string.Delete_address),
                                    color = MixinAppTheme.colors.tipError,
                                )
                                Spacer(Modifier.width(24.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_addr_remove),
                                    contentDescription = null,
                                    tint = MixinAppTheme.colors.tipError,
                                )
                            }
                        }
                    }
                }
            }
        }
        MixinButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp, bottom = 24.dp)
                .height(42.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
        ) {
            Text(stringResource(R.string.Add_address), fontSize = 14.sp)
        }
    }
}
