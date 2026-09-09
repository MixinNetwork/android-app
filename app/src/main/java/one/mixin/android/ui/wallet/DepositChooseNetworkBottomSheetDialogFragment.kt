package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.scale
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.databinding.FragmentDepositChooseNetworkBottomSheetBinding
import one.mixin.android.databinding.ItemChooseNetworkBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.util.getChainNetwork
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DepositChooseNetworkBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DepositChooseNetworkBottomSheetDialogFragment"
        private const val ASSET = "asset"
        private const val NAME = "name"
        private const val TOKENS = "tokens"
        private const val SELECTED_ASSET_ID = "selected_asset_id"

        fun newInstance(asset: TokenItem, name: String? = null) =
            DepositChooseNetworkBottomSheetDialogFragment().withArgs {
                putParcelable(ASSET, asset)
                putString(NAME, name)
            }

        fun newInstance(tokens: ArrayList<SwapToken>, selectedAssetId: String? = null) =
            DepositChooseNetworkBottomSheetDialogFragment().withArgs {
                putParcelableArrayList(TOKENS, tokens)
                putString(SELECTED_ASSET_ID, selectedAssetId)
            }
    }

    private val asset by lazy {
        requireArguments().getParcelableCompat(ASSET, TokenItem::class.java)
    }

    private val chainName by lazy {
        requireArguments().getString(NAME) ?: asset?.chainName
    }

    private val web3ViewModel by viewModels<Web3ViewModel>()

    private val binding by viewBinding(FragmentDepositChooseNetworkBottomSheetBinding::inflate)

    private val adapter by lazy {
        AssetAdapter(
            requireArguments().getParcelableArrayListCompat(TOKENS, SwapToken::class.java) ?: listOfNotNull(asset?.toSwapToken()),
            chainName ?: asset?.let { getChainNetwork(it.assetId, it.chainId, it.assetKey) },
            requireArguments().getString(SELECTED_ASSET_ID),
            showDepositNotice = !requireArguments().containsKey(TOKENS),
        )
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        dialog.setCancelable(requireArguments().containsKey(TOKENS))
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            val tokens = requireArguments().getParcelableArrayListCompat(TOKENS, SwapToken::class.java)
            if (tokens != null) {
                val walletId = tokens.firstOrNull()?.walletId
                if (walletId == null) {
                    walletName.setText(R.string.Privacy_Wallet)
                    walletName.isVisible = true
                } else {
                    lifecycleScope.launch {
                        walletName.text = web3ViewModel.getWalletName(walletId)
                        walletName.isVisible = !walletName.text.isNullOrBlank()
                    }
                }
            }
            assetRv.adapter = adapter
            adapter.callback = { token ->
                onTokenSelected?.invoke(token)
                callback?.invoke()
                dismiss()
            }
        }
    }

    var callback: (() -> Unit)? = null
    var onTokenSelected: ((SwapToken) -> Unit)? = null

    class AssetAdapter(
        private val tokens: List<SwapToken>,
        private val networkName: String?,
        private val selectedAssetId: String?,
        private val showDepositNotice: Boolean,
    ) : RecyclerView.Adapter<ItemHolder>() {
        var callback: ((SwapToken) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ItemHolder {
            return ItemHolder(
                ItemChooseNetworkBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
        }

        override fun getItemCount(): Int {
            return tokens.size + if (showDepositNotice) 1 else 0
        }

        internal fun tokenAt(position: Int): SwapToken? = tokens.getOrNull(position - if (showDepositNotice) 1 else 0)

        override fun onBindViewHolder(
            holder: ItemHolder,
            position: Int,
        ) {
            val token = tokenAt(position)
            holder.bind(token, networkName, token != null && token.assetId == selectedAssetId, !showDepositNotice, callback)
        }
    }

    class ItemHolder(val binding: ItemChooseNetworkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            tokenItem: SwapToken?,
            networkName: String? = null,
            selected: Boolean = false,
            showBalance: Boolean = false,
            callback: ((SwapToken) -> Unit)? = null,
        ) {
            binding.root.setOnClickListener(null)
            binding.content.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, if (selected) R.drawable.ic_check_blue_24dp else 0, 0)
            if (tokenItem == null) {
                binding.icon.isVisible = true
                binding.assetIcon.isVisible = false
                binding.content.setText(R.string.Choose_network_tip)
                binding.root.setBackgroundResource(R.drawable.bg_round_choose_network_yellow)
                binding.content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            } else {
                binding.icon.isVisible = false
                binding.assetIcon.isVisible = true
                if (showBalance) {
                    binding.assetIcon.loadToken(tokenItem.icon, tokenItem.chain.icon, tokenItem.collectionHash)
                } else {
                    binding.assetIcon.badge.isVisible = false
                    binding.assetIcon.bg.loadImage(tokenItem.chain.icon, R.drawable.ic_avatar_place_holder)
                }
                binding.content.text = buildSpannedString {
                    append(networkName ?: tokenItem.chain.name.takeIf { it.isNotBlank() }
                        ?: getChainNetwork(tokenItem.assetId, tokenItem.chain.chainId, tokenItem.address))
                    if (showBalance) {
                        append("\n")
                        scale(14f / 18f) {
                            color(binding.root.context.colorAttr(R.attr.text_assist)) {
                                append("${binding.root.context.getString(R.string.Balance)}: ${(tokenItem.balance?.takeIf { it.isNotBlank() } ?: "0").numberFormat8()} ${tokenItem.symbol}")
                            }
                        }
                    }
                }
                binding.root.setBackgroundResource(R.drawable.ripple_round_outline)
                binding.content.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                binding.root.setOnClickListener {
                    callback?.invoke(tokenItem)
                }
            }
        }
    }
}
