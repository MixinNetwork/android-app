package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentChainSelectionBottomSheetBinding
import one.mixin.android.databinding.ItemChainSelectionBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.withArgs
import one.mixin.android.repository.TokenRepository
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ChainItem
import one.mixin.android.widget.BottomSheet
import javax.inject.Inject

@AndroidEntryPoint
class ChainSelectionBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "ChainSelectionBottomSheetDialogFragment"
        private const val WALLET_ID = "wallet_id"

        fun newInstance(walletId: String) =
            ChainSelectionBottomSheetDialogFragment().withArgs {
                putString(WALLET_ID, walletId)
            }
    }

    @Inject
    lateinit var tokenRepository: TokenRepository

    private val walletId: String by lazy {
        requireArguments().getString(WALLET_ID)!!
    }

    private val binding by viewBinding(FragmentChainSelectionBottomSheetBinding::inflate)

    private val adapter = ChainAdapter()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        dialog.setCancelable(true)
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
            val maxHeight = (resources.displayMetrics.heightPixels * 0.8).toInt()
            binding.chainRv.layoutParams = binding.chainRv.layoutParams.apply {
                height = maxHeight
            }
        }

        binding.apply {
            rightIv.setOnClickListener {
                dismiss()
            }
            chainRv.adapter = adapter
            adapter.callback = { chainItem ->
                callback?.invoke(chainItem)
                dismiss()
            }
        }

        loadChainData()
    }

    private fun loadChainData() {
        lifecycleScope.launch {
            try {
                val chains = tokenRepository.getChainItemByWalletId(walletId)
                adapter.submitList(chains)
            } catch (_: Exception) {
                adapter.submitList(emptyList())
            }
        }
    }

    var callback: ((ChainItem) -> Unit)? = null

    class ChainAdapter : ListAdapter<ChainItem, ChainItemHolder>(ChainItem.DIFF_CALLBACK) {
        var callback: ((ChainItem) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ChainItemHolder {
            return ChainItemHolder(
                ItemChainSelectionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
        }

        override fun onBindViewHolder(
            holder: ChainItemHolder,
            position: Int,
        ) {
            getItem(position)?.let { holder.bind(it, callback) }
        }
    }

    class ChainItemHolder(val binding: ItemChainSelectionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            chainItem: ChainItem,
            callback: ((ChainItem) -> Unit)? = null,
        ) {
            binding.apply {
                root.setBackgroundResource(binding.root.context.theme.obtainStyledAttributes(intArrayOf(R.attr.bg_market_card)).getResourceId(0, 0))
                root.setPadding(16.dp, 4.dp, 16.dp, 4.dp)
                assetIcon.loadImage(
                    chainItem.iconUrl,
                    one.mixin.android.R.drawable.ic_avatar_place_holder,
                )
                nameTv.text = chainItem.name
                address.text = chainItem.destination.formatPublicKey(32)
                root.setOnClickListener {
                    callback?.invoke(chainItem)
                }
            }
        }
    }
}
