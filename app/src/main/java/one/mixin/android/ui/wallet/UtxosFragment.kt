package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentUtxosBinding
import one.mixin.android.databinding.ItemWalletUtxoBinding
import one.mixin.android.databinding.ViewWalletUtxoBottomBinding
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.textColorResource
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.ForceSyncOutputJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.UtxoItem
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet
import javax.inject.Inject

@AndroidEntryPoint
class UtxosFragment : BaseFragment() {
    companion object {
        fun newInstance(asset: TokenItem) =
            UtxosFragment().withArgs {
                putParcelable(ARGS_ASSET, asset)
            }

        private val UtxoItemDiffCallBack =
            object : DiffUtil.ItemCallback<UtxoItem>() {
                override fun areItemsTheSame(
                    oldItem: UtxoItem,
                    newItem: UtxoItem,
                ) =
                    oldItem.outputId == newItem.outputId

                override fun areContentsTheSame(
                    oldItem: UtxoItem,
                    newItem: UtxoItem,
                ) =
                    oldItem == newItem
            }
    }

    private val walletViewModel by viewModels<WalletViewModel>()

    private val asset by lazy { requireNotNull(requireArguments().getParcelableCompat(ARGS_ASSET, TokenItem::class.java)) }

    private var _binding: FragmentUtxosBinding? = null
    private val binding get() = requireNotNull(_binding) { "required _binding is null" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUtxosBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    private val adapter by lazy {
        UtxoAdapter()
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        binding.titleView.apply {
            titleTv.text = "${asset.name} UTXO"
            leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            rightAnimator.setOnClickListener {
                showBottom()
            }
        }
        binding.apply {
            utxoRv.adapter = adapter
        }
        walletViewModel.utxoItem(assetIdToAsset(asset.assetId)).observe(this.viewLifecycleOwner) {
            lifecycleScope.launch {
                adapter.submitData(it)
            }
        }
    }

    class UtxoAdapter :
        PagingDataAdapter<UtxoItem, UtxoHolder>(UtxoItemDiffCallBack) {
        override fun onBindViewHolder(
            holder: UtxoHolder,
            position: Int,
        ) {
            getItem(position)?.let {
                holder.bind(it)
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): UtxoHolder {
            return UtxoHolder(ItemWalletUtxoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    class UtxoHolder(val binding: ItemWalletUtxoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UtxoItem) {
            binding.apply {
                name.text = item.outputId
                hash.text = item.transactionHash
                value.text = item.amount
                value.textColorResource = if (item.state == "unspent") R.color.wallet_green else R.color.wallet_pink
            }
            binding.root.setOnLongClickListener {
                it.context?.getClipboardManager()
                    ?.setPrimaryClip(ClipData.newPlainText(null, item.transactionHash))
                toast(R.string.copied_to_clipboard)
                true
            }
        }
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomBinding = ViewWalletUtxoBottomBinding.bind(View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_wallet_utxo_bottom, null))
        builder.setCustomView(bottomBinding.root)
        val bottomSheet = builder.create()
        bottomBinding.apply {
            refresh.setOnClickListener {
                jobManager.addJobInBackground(ForceSyncOutputJob(0, assetIdToAsset(asset.assetId)))
                bottomSheet.dismiss()
            }
            cancel.setOnClickListener { bottomSheet.dismiss() }
        }
        bottomSheet.show()
    }
}
