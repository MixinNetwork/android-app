package one.mixin.android.ui.home.web3.trade

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.databinding.FragmentMarketListBottomSheetBinding
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import javax.inject.Inject

@AndroidEntryPoint
class MarketListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "MarketListBottomSheetDialogFragment"
        private const val ARGS_IS_LONG = "args_is_long"

        fun newInstance(isLong: Boolean) = MarketListBottomSheetDialogFragment().withArgs {
            putBoolean(ARGS_IS_LONG, isLong)
        }
    }

    private val binding by viewBinding(FragmentMarketListBottomSheetBinding::inflate)
    private val adapter by lazy { MarketListAdapter { market -> onMarketClick(market) } }

    @Inject
    lateinit var perpsMarketDao: PerpsMarketDao

    private val isLong by lazy { requireArguments().getBoolean(ARGS_IS_LONG, true) }
    private var allMarkets = listOf<PerpsMarket>()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root

        binding.ph.doOnPreDraw {
            binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
                height = binding.ph.getSafeAreaInsetsTop() + requireContext().appCompatActionBarHeight()
            }
        }

        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            closeIb.setOnClickListener {
                dismiss()
            }

            marketRv.layoutManager = LinearLayoutManager(requireContext())
            marketRv.adapter = adapter

            searchEt.listener = object : one.mixin.android.widget.SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    filterMarkets(s?.toString() ?: "")
                }

                override fun onSearch() {}
            }
        }

        loadLocalMarkets()
    }

    private fun loadLocalMarkets() {
        lifecycleScope.launch {
            allMarkets = withContext(Dispatchers.IO) {
                perpsMarketDao.getAllMarkets()
            }
            updateList(allMarkets)
        }
    }

    private fun filterMarkets(query: String) {
        if (query.isEmpty()) {
            updateList(allMarkets)
        } else {
            val filtered = allMarkets.filter { market ->
                market.displaySymbol.contains(query, ignoreCase = true) ||
                market.symbol.contains(query, ignoreCase = true)
            }
            updateList(filtered)
        }
    }

    private fun updateList(markets: List<PerpsMarket>) {
        binding.rvVa.displayedChild = if (markets.isEmpty()) 1 else 0
        adapter.submitList(markets)
    }

    private fun onMarketClick(market: PerpsMarket) {
        PerpsActivity.showOpenPosition(
            context = requireContext(),
            marketId = market.marketId,
            marketSymbol = market.symbol,
            marketDisplaySymbol = market.displaySymbol,
            isLong = isLong
        )
        dismiss()
    }
}
