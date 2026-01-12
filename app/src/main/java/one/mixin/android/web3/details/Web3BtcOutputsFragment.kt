package one.mixin.android.web3.details

import android.app.Dialog
import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.databinding.FragmentWeb3BtcOutputsBinding
import one.mixin.android.databinding.ItemWalletUtxoBinding
import one.mixin.android.databinding.ViewWalletUtxoBottomBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.textColorResource
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshWeb3BitCoinJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.widget.BottomSheet
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class Web3BtcOutputsFragment : BaseFragment() {
    companion object {
        const val ARGS_WALLET_ID: String = "args_wallet_id"
        const val ARGS_ADDRESS: String = "args_address"

        fun newInstance(walletId: String, address: String) = Web3BtcOutputsFragment().withArgs {
            putString(ARGS_WALLET_ID, walletId)
            putString(ARGS_ADDRESS, address)
        }
    }

    private val walletId: String by lazy { requireNotNull(requireArguments().getString(ARGS_WALLET_ID)) }
    private val address: String by lazy { requireNotNull(requireArguments().getString(ARGS_ADDRESS)) }

    private val web3ViewModel by viewModels<Web3ViewModel>()

    @Inject
    lateinit var jobManager: MixinJobManager

    private var _binding: FragmentWeb3BtcOutputsBinding? = null
    private val binding get() = requireNotNull(_binding) { "required _binding is null" }

    private val adapter: OutputsAdapter = OutputsAdapter(
        onLongClick = { output ->
            showOutputActions(output)
        },
    )

    private var loadingDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWeb3BtcOutputsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.apply {
            titleTv.setTextOnly("BTC UTXO")
            leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            rightAnimator.isVisible = true
            rightAnimator.setOnClickListener {
                showBottom()
            }
        }
        binding.outputsRv.adapter = adapter
        refreshOutputs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refreshOutputs() {
        lifecycleScope.launch(Dispatchers.IO) {
            val outputs: List<WalletOutput> = web3ViewModel.outputsByAddress(address, Constants.ChainId.BITCOIN_CHAIN_ID)
            launch(Dispatchers.Main) {
                adapter.submitList(outputs)
            }
        }
    }

    private fun showOutputActions(output: WalletOutput) {
        alertDialogBuilder()
            .setMessage(getString(R.string.Remove_UTXO))
            .setNegativeButton(R.string.Copy_hash) { dialog, _ ->
                requireContext().getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, output.transactionHash))
                toast(R.string.copied_to_clipboard)
                dialog.dismiss()
            }
            .setPositiveButton(R.string.Cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomBinding = ViewWalletUtxoBottomBinding.bind(
            View.inflate(
                ContextThemeWrapper(requireActivity(), R.style.Custom),
                R.layout.view_wallet_utxo_bottom,
                null,
            ),
        )
        builder.setCustomView(bottomBinding.root)
        val bottomSheet = builder.create()
        bottomBinding.apply {
            refresh.setOnClickListener {
                forceRefreshOutputs()
                bottomSheet.dismiss()
            }
            cancel.setOnClickListener { bottomSheet.dismiss() }
        }
        bottomSheet.show()
    }

    private fun forceRefreshOutputs() {
        lifecycleScope.launch {
            loadingDialog?.dismiss()
            loadingDialog = null
            loadingDialog = indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply { setCancelable(false) }
            loadingDialog?.show()
            runCatching {
                jobManager.addJobInBackground(RefreshWeb3BitCoinJob(walletId))
            }.onFailure { err ->
                Timber.e(err)
            }
            refreshOutputs()
            loadingDialog?.dismiss()
            loadingDialog = null
        }
    }

    private class OutputsAdapter(
        private val onLongClick: (WalletOutput) -> Unit,
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<OutputsHolder>() {
        private val items: MutableList<WalletOutput> = mutableListOf()

        fun submitList(outputs: List<WalletOutput>) {
            items.clear()
            items.addAll(outputs)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutputsHolder {
            return OutputsHolder(ItemWalletUtxoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: OutputsHolder, position: Int) {
            holder.bind(items[position], onLongClick)
        }

        override fun getItemCount(): Int = items.size
    }

    private class OutputsHolder(private val binding: ItemWalletUtxoBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WalletOutput, onLongClick: (WalletOutput) -> Unit) {
            binding.apply {
                name.text = item.outputId
                hash.text = item.transactionHash
                value.text = item.amount
                value.textColorResource = if (item.status == "unspent") R.color.wallet_green else R.color.wallet_pink
            }
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }
}
