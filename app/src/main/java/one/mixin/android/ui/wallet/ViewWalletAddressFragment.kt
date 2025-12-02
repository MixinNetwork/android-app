package one.mixin.android.ui.wallet

import PageScaffold
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.landing.components.HighlightedTextWithClick
import one.mixin.android.ui.wallet.alert.components.cardBackground

@AndroidEntryPoint
class ViewWalletAddressFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val walletId = arguments?.getString(WalletSecurityActivity.EXTRA_WALLET_ID) ?: throw IllegalArgumentException("Wallet ID is required")
        return ComposeView(requireContext()).apply {
            setContent {
                ViewAddressScreen(walletId, learnMoreAction = {
                    context.openUrl(getString(R.string.watch_wallet_url))
                }) {
                    requireActivity().finish()
                }
            }
        }
    }

    companion object {
        fun newInstance(walletId: String?): ViewWalletAddressFragment {
            val fragment = ViewWalletAddressFragment()
            val args = Bundle()
            args.putString(WalletSecurityActivity.EXTRA_WALLET_ID, walletId)
            fragment.arguments = args
            return fragment
        }
    }
}

@Composable
fun ViewAddressScreen(walletId: String, learnMoreAction: () -> Unit, pop: () -> Unit) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<Web3ViewModel>()
    var addresses by remember { mutableStateOf(emptyList<Web3Address>()) }

    LaunchedEffect(walletId) {
        addresses = viewModel.getAddressesGroupedByDestination(walletId)
    }

    MixinAppTheme {
        PageScaffold(
            title = "",
            verticalScrollable = true,
            pop = pop,
            actions = {
                IconButton(onClick = { context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_support),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon
                    )
                }
            }
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .cardBackground(
                        Color.Transparent, MixinAppTheme.colors.borderColor, cornerRadius = 8.dp
                    )
                    .padding(
                        horizontal = 16.dp,
                    )
                    .padding(top = 40.dp, bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(painter = painterResource(R.drawable.ic_watch_wallet_address), tint = Color.Unspecified, contentDescription = null)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    stringResource(R.string.Watch_Wallet), fontSize = 18.sp, fontWeight = FontWeight.W600, color = MixinAppTheme.colors.textPrimary, textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                HighlightedTextWithClick(
                    stringResource(R.string.watch_address_description), modifier = Modifier.align(Alignment.CenterHorizontally), stringResource(R.string.Learn_More), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 21.sp
                ) {
                    learnMoreAction.invoke()
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Address list section
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .cardBackground(
                        Color.Transparent, MixinAppTheme.colors.borderColor, cornerRadius = 8.dp
                    )
                    .padding(vertical = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.Address), fontSize = 16.sp, color = MixinAppTheme.colors.textAssist, modifier = Modifier
                        .padding(bottom = 6.dp)
                        .padding(horizontal = 16.dp)
                )


                addresses.forEach { address ->
                    AddressItem(address)
                }
            }
        }
    }
}

@Composable
private fun AddressItem(
    address: Web3Address,
) {
    val context = LocalContext.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = address.destination,
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Wallet Address", address.destination)
                    clipboard.setPrimaryClip(clip)
                    toast(R.string.copied_to_clipboard)
                },
                modifier = Modifier
                    .size(16.dp)
                    .offset(y = (6).dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy_gray),
                    tint = Color.Unspecified,
                    contentDescription = stringResource(R.string.Copy),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        val chains = if (address.chainId == Constants.ChainId.SOLANA_CHAIN_ID) {
            listOf(R.drawable.ic_chain_sol)
        } else {
            listOf(
                R.drawable.ic_chain_eth,
                R.drawable.ic_chain_polygon,
                R.drawable.ic_chain_bsc,
                R.drawable.ic_chain_base,
                R.drawable.ic_chain_arbitrum_eth,
                R.drawable.ic_chain_optimism,
            )
        }
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            chains!!.forEachIndexed { index, iconRes ->
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(19.dp)
                        .offset(x = (-6 * index).dp)
                        .clip(CircleShape)
                        .border(1.dp, MixinAppTheme.colors.background, CircleShape)
                        .padding(0.5.dp)
                )
            }
        }
    }
}