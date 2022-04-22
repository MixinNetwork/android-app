package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.databinding.FragmentCurrencyBottomSheetBinding
import one.mixin.android.databinding.ItemCurrencyBinding
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
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        context?.let { c ->
            val topOffset = c.statusBarHeight() + c.appCompatActionBarHeight()
            binding.root.heightOffset = topOffset
        }
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            closeIv.setOnClickListener { dismiss() }
            searchEt.listener = object : SearchView.OnSearchViewListener {
                override fun afterTextChanged(s: Editable?) {
                    filter(s.toString())
                }

                override fun onSearch() {
                }
            }
            currencyAdapter.currencyListener = object : OnCurrencyListener {
                override fun onClick(currency: Currency) {
                    savePreference(currency)
                }
            }
            currencyRv.adapter = currencyAdapter
        }
        setListData()
    }

    private fun savePreference(currency: Currency) = lifecycleScope.launch {
        val pb = indeterminateProgressDialog(
            message = R.string.Please_wait_a_bit,
            title = R.string.Switching_currency
        ).apply {
            setCancelable(false)
        }
        pb.show()

        handleMixinResponse(
            invokeNetwork = {
                bottomViewModel.preferences(
                    AccountUpdateRequest(fiatCurrency = currency.name)
                )
            },
            successBlock = {
                it.data?.let { account ->
                    Session.storeAccount(account)
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
            }
        )
    }

    private fun setListData() {
        val names = resources.getStringArray(R.array.currency_names)
        val symbols = resources.getStringArray(R.array.currency_symbols).map { it.trim() }
        val flagArray = resources.obtainTypedArray(R.array.currency_flags)
        val flags = arrayListOf<Int>()
        for (i in names.indices) {
            flags.add(flagArray.getResourceId(i, 0))
        }
        flagArray.recycle()

        if (flags.size != names.size || flags.size != symbols.size || names.size != symbols.size) {
            return
        }
        currencies.clear()
        for (i in 0 until flags.size) {
            currencies.add(Currency(names[i], symbols[i], flags[i]))
        }
        currencyAdapter.submitList(currencies)
    }

    private fun filter(s: String) {
        currencyAdapter.submitList(
            if (s.isNotBlank()) {
                currencies.filter {
                    it.name.containsIgnoreCase(s)
                }.sortedByDescending { it.name.equalsIgnoreCase(s) }
            } else currencies
        )
    }

    interface Callback {
        fun onCurrencyClick(currency: Currency)
    }
}

class CurrencyAdapter : ListAdapter<Currency, CurrencyHolder>(Currency.DIFF_CALLBACK) {
    var currencyListener: OnCurrencyListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CurrencyHolder(ItemCurrencyBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CurrencyHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, currencyListener) }
    }
}

class CurrencyHolder(private val itemBinding: ItemCurrencyBinding) : RecyclerView.ViewHolder(itemBinding.root) {
    fun bind(currency: Currency, listener: OnCurrencyListener?) {
        itemBinding.apply {
            if (currency.name == Session.getFiatCurrency()) {
                checkIv.isVisible = true
            } else {
                checkIv.isInvisible = true
            }
            flagIv.setImageResource(currency.flag)
            name.text = itemView.context.getString(R.string.wallet_setting_currency_desc, currency.name, currency.symbol)
        }
        itemView.setOnClickListener { listener?.onClick(currency) }
    }
}

interface OnCurrencyListener {
    fun onClick(currency: Currency)
}

data class Currency(
    val name: String,
    val symbol: String,
    val flag: Int
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Currency>() {
            override fun areItemsTheSame(oldItem: Currency, newItem: Currency) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: Currency, newItem: Currency) =
                oldItem == newItem
        }
    }
}
