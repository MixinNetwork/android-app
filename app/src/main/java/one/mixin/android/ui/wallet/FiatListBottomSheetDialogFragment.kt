package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentFiatListBottomSheetBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.setting.CurrencyAdapter
import one.mixin.android.ui.setting.OnCurrencyListener
import one.mixin.android.ui.setting.getCurrencyData
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_CURRENCY
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView

@AndroidEntryPoint
class FiatListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "FiatListBottomSheetDialogFragment"
        const val ARGS_CURRENCIES = "args_currencies"
        fun newInstance(currency: Currency, list: List<Currency>? = null) = FiatListBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_CURRENCY, currency)
            putParcelableArrayList(ARGS_CURRENCIES, list?.let { ArrayList(it) })
        }
    }

    private val binding by viewBinding(FragmentFiatListBottomSheetBinding::inflate)

    private val currencyAdapter = CurrencyAdapter()
    private val currencies = arrayListOf<Currency>()

    var callback: Callback? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight() + requireContext().appCompatActionBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        val currency = requireNotNull(requireArguments().getParcelableCompat(ARGS_CURRENCY, Currency::class.java))
        binding.apply {
            closeIb.setOnClickListener {
                searchEt.hideKeyboard()
                dismiss()
            }
            currencyAdapter.currentCurrency = currency.name
            fiatRv.adapter = currencyAdapter
            currencyAdapter.currencyListener = object : OnCurrencyListener {
                override fun onClick(currency: Currency) {
                    callback?.onCurrencyClick(currency)
                    dismiss()
                }
            }
            searchEt.setHint(getString(R.string.search_placeholder_asset))
            searchEt.listener = object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    filter(s.toString())
                }

                override fun onSearch() {}
            }
        }
        currencies.clear()
        currencies.addAll(
            requireArguments().getParcelableArrayListCompat(
                ARGS_CURRENCIES,
                Currency::class.java,
            ) ?: getCurrencyData(requireContext().resources),
        )
        currencyAdapter.submitList(currencies)
    }

    private fun filter(s: String) {
        currencyAdapter.submitList(
            if (s.isNotBlank()) {
                currencies.filter {
                    it.name.containsIgnoreCase(s)
                }.sortedByDescending { it.name.equalsIgnoreCase(s) }
            } else {
                currencies
            },
        )
    }

    interface Callback {
        fun onCurrencyClick(currency: Currency)
    }
}
