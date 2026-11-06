package one.mixin.android.ui.wallet.network

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSelectListBottomSheetBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class NetworkMultiSelectBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    override val centerOnExpandedScreen = true

    private val binding by viewBinding(FragmentSelectListBottomSheetBinding::inflate)
    private val viewModel by viewModels<NetworkIconViewModel>()

    var onApply: ((List<String>) -> Unit)? = null

    private val networks: List<NetworkOption> by lazy {
        requireArguments().getParcelableArrayListCompat(ARGS_NETWORKS, NetworkOption::class.java) ?: arrayListOf()
    }

    private val selectedChainIds by lazy {
        val initialIds = requireArguments().getStringArrayList(ARGS_SELECTED_CHAIN_IDS).orEmpty()
        networks.map { it.chainId }.filter { it in initialIds }.toMutableSet()
    }

    private val adapter by lazy { SelectableNetworkAdapter(selectedChainIds, ::toggleNetwork) }
    private val selectedAdapter by lazy { SelectedNetworkAdapter(::toggleNetwork) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.doOnPreDraw {
            binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
                height = binding.ph.getSafeAreaInsetsTop() + requireContext().appCompatActionBarHeight()
            }
        }
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            ph.setOnClickListener {
                searchEt.hideKeyboard()
                dismiss()
            }
            closeIb.isVisible = false
            searchEt.updateLayoutParams<RelativeLayout.LayoutParams> {
                removeRule(RelativeLayout.END_OF)
                marginStart = resources.getDimensionPixelSize(R.dimen.margin16)
            }
            searchEt.setBackgroundResource(R.drawable.bg_round_window_24dp)
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.margin12)
            val verticalPadding = resources.getDimensionPixelSize(R.dimen.margin10)
            searchEt.setPaddingRelative(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            searchEt.et.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            searchEt.et.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_edit_search, 0, 0, 0)
            searchEt.setHint(R.string.Name)
            rv.adapter = adapter
            selectRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            selectRv.adapter = selectedAdapter
            selectedAdapter.networks = networks.filter { it.chainId in selectedChainIds }
            selectRv.isVisible = selectedAdapter.networks.isNotEmpty()
            resetButton.setOnClickListener {
                searchEt.hideKeyboard()
                onApply?.invoke(emptyList())
                dismiss()
            }
            applyButton.setOnClickListener {
                searchEt.hideKeyboard()
                onApply?.invoke(networks.map { it.chainId }.filter { it in selectedChainIds })
                dismiss()
            }
            searchEt.listener = object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    filter(s?.toString().orEmpty())
                }

                override fun onSearch() {}
            }
        }
        filter(binding.searchEt.et.text?.toString().orEmpty())
        lifecycleScope.launch {
            val iconUrls = viewModel.iconUrls(networks.map { it.chainId }.toSet())
            adapter.iconUrls = iconUrls
            selectedAdapter.iconUrls = iconUrls
            adapter.notifyDataSetChanged()
            selectedAdapter.notifyDataSetChanged()
        }
    }

    private fun filter(query: String) {
        adapter.networks = filterNetworks(networks, query)
        adapter.notifyDataSetChanged()
        binding.rvVa.displayedChild = if (adapter.networks.isEmpty()) POS_EMPTY_SEARCH else POS_NETWORKS
        binding.rv.scrollToPosition(0)
    }

    private fun toggleNetwork(network: NetworkOption) {
        binding.searchEt.hideKeyboard()
        val added = selectedChainIds.add(network.chainId)
        if (!added) {
            selectedChainIds.remove(network.chainId)
        }
        val position = adapter.networks.indexOf(network)
        if (position >= 0) {
            adapter.notifyItemChanged(position)
        }
        selectedAdapter.networks = networks.filter { it.chainId in selectedChainIds }
        selectedAdapter.notifyDataSetChanged()
        binding.selectRv.isVisible = selectedAdapter.networks.isNotEmpty()
        if (added) {
            binding.selectRv.scrollToPosition(selectedAdapter.networks.indexOf(network))
        }
    }

    companion object {
        const val TAG = "NetworkMultiSelectBottomSheetDialogFragment"
        private const val ARGS_NETWORKS = "args_networks"
        private const val ARGS_SELECTED_CHAIN_IDS = "args_selected_chain_ids"
        private const val POS_NETWORKS = 0
        private const val POS_EMPTY_SEARCH = 2

        fun newInstance(
            networks: ArrayList<NetworkOption>,
            selectedChainIds: ArrayList<String>,
        ): NetworkMultiSelectBottomSheetDialogFragment {
            return NetworkMultiSelectBottomSheetDialogFragment().withArgs {
                putParcelableArrayList(ARGS_NETWORKS, networks)
                putStringArrayList(ARGS_SELECTED_CHAIN_IDS, selectedChainIds)
            }
        }
    }
}
