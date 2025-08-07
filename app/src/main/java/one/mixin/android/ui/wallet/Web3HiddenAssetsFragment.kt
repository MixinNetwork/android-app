package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentHiddenAssetsBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.config
import one.mixin.android.extension.dp
import one.mixin.android.extension.navigate
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.wallet.adapter.AssetItemCallback
import one.mixin.android.ui.wallet.adapter.WalletWeb3TokenAdapter
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.details.Web3TransactionsFragment
import kotlin.math.abs

@AndroidEntryPoint
class Web3HiddenAssetsFragment : BaseFragment(R.layout.fragment_hidden_assets), HeaderAdapter.OnItemListener {
    companion object {
        val TAG = Web3HiddenAssetsFragment::class.java.simpleName

        fun newInstance(walletId: String? = null) = Web3HiddenAssetsFragment().apply {
            arguments = Bundle().apply {
                putString(ARGS_WALLET_ID, walletId)
            }
        }

        const val POS_ASSET = 0
        const val POS_EMPTY = 1
        const val ARGS_WALLET_ID = "args_wallet_id"
    }

    private val web3ViewModel by viewModels<Web3ViewModel>()
    private val binding by viewBinding(FragmentHiddenAssetsBinding::bind)

    private val walletId by lazy { requireNotNull(requireArguments().getString(ARGS_WALLET_ID)) }

    private var assets: List<Web3TokenItem> = listOf()
    private val assetsAdapter by lazy { WalletWeb3TokenAdapter(true) }

    private var distance = 0
    private var snackbar: Snackbar? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        assetsAdapter.onItemListener = this
        binding.apply {
            titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            ItemTouchHelper(
                AssetItemCallback(
                    object : AssetItemCallback.ItemCallbackListener {
                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder) {
                            val hiddenPos = viewHolder.absoluteAdapterPosition
                            val asset = assetsAdapter.data!![assetsAdapter.getPosition(hiddenPos)]
                            val deleteItem = assetsAdapter.removeItem(hiddenPos)!!
                            lifecycleScope.launch {
                                web3ViewModel.updateTokenHidden(asset.assetId, asset.walletId, false)
                                val anchorView = assetsRv

                                snackbar =
                                    Snackbar.make(anchorView, getString(R.string.wallet_already_shown, asset.symbol), 3500)
                                        .setAction(R.string.UNDO) {
                                            assetsAdapter.restoreItem(deleteItem, hiddenPos)
                                            lifecycleScope.launch {
                                                web3ViewModel.updateTokenHidden(asset.assetId, asset.walletId, true)
                                            }
                                        }.setActionTextColor(ContextCompat.getColor(requireContext(), R.color.wallet_blue)).apply {
                                            (this.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text))
                                                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                                        }.apply {
                                            snackbar?.config(anchorView.context)
                                        }
                                snackbar?.show()
                                distance = 0
                            }
                        }
                    },
                ),
            ).apply { attachToRecyclerView(assetsRv) }
            assetsRv.adapter = assetsAdapter
            assetsRv.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(
                        recyclerView: RecyclerView,
                        dx: Int,
                        dy: Int,
                    ) {
                        if (abs(distance) > 50.dp && snackbar?.isShown == true) {
                            snackbar?.dismiss()
                            distance = 0
                        }
                        distance += dy
                    }
                },
            )

            web3ViewModel.hiddenAssetItems(walletId).observe(
                viewLifecycleOwner,
            ) { hiddenTokens ->
                if (hiddenTokens != null && hiddenTokens.isNotEmpty()) {
                    assetsVa.displayedChild = POS_ASSET
                    assets = hiddenTokens
                    assetsAdapter.setAssetList(hiddenTokens)
                } else {
                    assetsVa.displayedChild = POS_EMPTY
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        snackbar?.dismiss()
    }

    override fun <T> onNormalItemClick(item: T) {
        val token = item as Web3TokenItem
        lifecycleScope.launch {
            if (walletId == null) {
                toast(R.string.Data_error)
                return@launch
            }
            val address = web3ViewModel.getAddressesByChainId(walletId!!, token.chainId)
            if (address != null) {
                view?.navigate(
                    R.id.action_web3_hidden_assets_to_web3_transactions,
                    Bundle().apply {
                        putString(Web3TransactionsFragment.ARGS_ADDRESS, address.destination)
                        putParcelable(Web3TransactionsFragment.ARGS_TOKEN, token)
                    }
                )
            } else {
                toast(R.string.Data_error)
            }
        }
    }
}
