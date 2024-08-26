package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.databinding.FragmentCurrencyBottomSheetBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView

@AndroidEntryPoint
class CurrencyBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "CurrencyBottomSheetDialogFragment"

        fun newInstance() = CurrencyBottomSheetDialogFragment()
    }

    var callback: Callback? = null

    private val currencyAdapter = CurrencyAdapter()
    private val currencies = arrayListOf<Currency>()

    private val binding by viewBinding(FragmentCurrencyBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        context?.let { c ->
            val topOffset = c.statusBarHeight() + c.appCompatActionBarHeight()
            binding.root.heightOffset = topOffset
        }
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            closeIv.setOnClickListener { dismiss() }
            searchEt.listener =
                object : SearchView.OnSearchViewListener {
                    override fun afterTextChanged(s: Editable?) {
                        filter(s.toString())
                    }

                    override fun onSearch() {
                    }
                }
            currencyAdapter.currencyListener =
                object : OnCurrencyListener {
                    override fun onClick(currency: Currency) {
                        savePreference(currency)
                    }
                }
            currencyRv.adapter = currencyAdapter
        }
        currencies.clear()
        currencies.addAll(getCurrencyData(requireContext().resources))
        currencyAdapter.submitList(currencies)
    }

    private fun savePreference(currency: Currency) =
        lifecycleScope.launch {
            val pb =
                indeterminateProgressDialog(
                    message = R.string.Please_wait_a_bit,
                    title = R.string.Switching_currency,
                ).apply {
                    setCancelable(false)
                }
            pb.show()

            handleMixinResponse(
                invokeNetwork = {
                    bottomViewModel.preferences(
                        AccountUpdateRequest(fiatCurrency = currency.name),
                    )
                },
                successBlock = {
                    it.data?.let { account ->
                        Session.storeAccount(account, 10)
                        callback?.onCurrencyClick(currency)
                        toast(R.string.Save_success)
                        dismiss()
                    }
                },
                doAfterNetworkSuccess = {
                    pb.dismiss()
                },
                exceptionBlock = {
                    pb.dismiss()
                    return@handleMixinResponse false
                },
            )
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
