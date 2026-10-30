package one.mixin.android.ui.address

import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.GetActionBarHeight
import one.mixin.android.compose.GetStatusBarHeightValue
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.address.component.SearchTextField
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.ui.wallet.network.NetworkMultiSelectBottomSheetDialogFragment
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.Address
import one.mixin.android.vo.Chain
import one.mixin.android.vo.displayAddress
import one.mixin.android.widget.bottomSheetHeight
import one.mixin.android.widget.components.MixinButton

@AndroidEntryPoint
class AddressBookBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    override val centerOnExpandedScreen = true

    private val viewModel by viewModels<AddressViewModel>()

    override fun getTheme() = R.style.AppTheme_Dialog

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.refreshAddressBook()
    }

    override fun getBottomSheetHeight(view: View): Int =
        requireContext().bottomSheetHeight() - view.getSafeAreaInsetsTop() - requireContext().appCompatActionBarHeight()

    override fun showError(error: String) {}

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val addressFlow = remember { viewModel.allAddressesFlow() }
            val addresses by addressFlow.collectAsStateWithLifecycle(initialValue = null)
            var chains by remember { mutableStateOf<List<Chain>>(emptyList()) }
            var query by rememberSaveable { mutableStateOf("") }
            var selectedChainIds by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
            LaunchedEffect(addresses) {
                chains = viewModel.getChains()
            }
            val filteredAddresses = remember(addresses, selectedChainIds, query) {
                addresses?.filter { address ->
                    (selectedChainIds.isEmpty() || address.chainId in selectedChainIds) &&
                        (query.isBlank() || address.label.contains(query, ignoreCase = true) ||
                            address.displayAddress().contains(query, ignoreCase = true))
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LocalConfiguration.current.screenHeightDp.dp - GetActionBarHeight() - GetStatusBarHeightValue())
                    .background(MixinAppTheme.colors.background)
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        backgroundColor = MixinAppTheme.colors.backgroundWindow,
                        hint = stringResource(R.string.address_book_search_hint),
                    )
                    Text(
                        text = stringResource(R.string.Cancel),
                        color = MixinAppTheme.colors.textBlue,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { dismiss() }.padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor, cornerRadius = 24.dp)
                        .clickable {
                            requireView().hideKeyboard()
                            NetworkMultiSelectBottomSheetDialogFragment.newInstance(
                                ArrayList(addressBookNetworks),
                                selectedChainIds,
                            ).apply {
                                onApply = { selectedChainIds = ArrayList(it) }
                            }.show(childFragmentManager, NetworkMultiSelectBottomSheetDialogFragment.TAG)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectedChainIds.size == 1) {
                        AddressBookNetwork(selectedChainIds.first(), chains.firstOrNull { it.chainId == selectedChainIds.first() })
                    } else {
                        if (selectedChainIds.size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                                selectedChainIds.take(2).forEach { chainId ->
                                    CoilImage(
                                        chains.firstOrNull { it.chainId == chainId }?.iconUrl,
                                        R.drawable.ic_avatar_place_holder,
                                        Modifier.size(16.dp).clip(CircleShape),
                                    )
                                }
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            if (selectedChainIds.isEmpty()) stringResource(R.string.All_Networks)
                            else stringResource(R.string.number_of_networks, selectedChainIds.size),
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 14.sp,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(painterResource(R.drawable.ic_sort_arrow_down), contentDescription = null, tint = Color.Unspecified)
                }
                if (filteredAddresses.isNullOrEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (filteredAddresses == null) {
                            CircularProgressIndicator(color = MixinAppTheme.colors.accent)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(painterResource(R.drawable.ic_empty_file), contentDescription = null)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    stringResource(if (query.isBlank()) R.string.address_book_no_records else R.string.address_book_no_records_found),
                                    color = MixinAppTheme.colors.textAssist,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filteredAddresses, key = { it.addressId }) { address ->
                            AddressBookCard(address, chains.firstOrNull { it.chainId == address.chainId }, onClick = {
                                requireView().hideKeyboard()
                                WalletActivity.showAddressBookDetail(requireActivity(), address, arguments?.getString(WalletActivity.ARGS_WALLET_ID))
                            })
                        }
                    }
                }
                MixinButton(
                    onClick = {
                        requireView().hideKeyboard()
                        AnalyticsTracker.trackAddressBookAddStart()
                        WalletActivity.showAddressBookAdd(requireActivity(), defaultAddressBookNetwork(selectedChainIds).chainId)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp, bottom = 24.dp).height(42.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                ) {
                    Text(stringResource(R.string.Add_address), fontSize = 14.sp)
                }
            }
        }
    }

    companion object {
        const val TAG = "AddressBookBottomSheetDialogFragment"

        fun newInstance(walletId: String?) = AddressBookBottomSheetDialogFragment().withArgs {
            putString(WalletActivity.ARGS_WALLET_ID, walletId)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AddressBookCard(address: Address, chain: Chain?, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                address.label,
                modifier = Modifier.weight(1f),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            AddressBookNetwork(address.chainId, chain)
        }
        Text(address.displayAddress(), color = MixinAppTheme.colors.textAssist, fontSize = 12.sp)
        Text(
            stringResource(R.string.Last_Updated_at, address.updatedAt.fullDate()),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 11.sp,
        )
    }
}
