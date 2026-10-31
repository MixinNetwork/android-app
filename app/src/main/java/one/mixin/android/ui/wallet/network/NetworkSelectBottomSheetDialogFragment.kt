package one.mixin.android.ui.wallet.network

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.wallet.SearchBar
import one.mixin.android.widget.bottomSheetHeight

@AndroidEntryPoint
class NetworkSelectBottomSheetDialogFragment : MixinComposeBottomSheetDialogFragment() {
    override val centerOnExpandedScreen = true

    private val viewModel by viewModels<NetworkIconViewModel>()

    var onNetworkSelected: ((NetworkOption) -> Unit)? = null

    private val networks: List<NetworkOption> by lazy {
        requireArguments().getParcelableArrayListCompat(ARGS_NETWORKS, NetworkOption::class.java) ?: arrayListOf()
    }

    private val selectedChainId: String? by lazy {
        requireArguments().getString(ARGS_SELECTED_CHAIN_ID)
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
        MixinAppTheme {
            val iconUrls = rememberNetworkIconUrls(viewModel, networks)
            NetworkSelectScreen(
                networks = networks,
                selectedChainId = selectedChainId,
                iconUrls = iconUrls,
                onCancel = { dismiss() },
                onSelect = { option ->
                    onNetworkSelected?.invoke(option)
                    dismiss()
                },
            )
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().bottomSheetHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {}

    companion object {
        const val TAG = "NetworkSelectBottomSheetDialogFragment"
        private const val ARGS_NETWORKS = "args_networks"
        private const val ARGS_SELECTED_CHAIN_ID = "args_selected_chain_id"

        fun newInstance(
            networks: ArrayList<NetworkOption>,
            selectedChainId: String?,
        ): NetworkSelectBottomSheetDialogFragment {
            return NetworkSelectBottomSheetDialogFragment().withArgs {
                putParcelableArrayList(ARGS_NETWORKS, networks)
                putString(ARGS_SELECTED_CHAIN_ID, selectedChainId)
            }
        }
    }
}

@Composable
private fun NetworkSelectScreen(
    networks: List<NetworkOption>,
    selectedChainId: String?,
    iconUrls: Map<String, String>,
    onCancel: () -> Unit,
    onSelect: (NetworkOption) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visibleNetworks = remember(networks, query) { filterNetworks(networks, query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MixinAppTheme.colors.background),
    ) {
        SearchBar(
            query = query,
            onQueryChanged = { query = it },
            isSearching = false,
            onCancel = onCancel,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            visibleNetworks.forEach { network ->
                NetworkOptionRow(
                    name = network.name,
                    iconUrl = iconUrls[network.chainId],
                    onClick = { onSelect(network) },
                    trailing = {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (network.chainId == selectedChainId) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_blue_24dp),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}
