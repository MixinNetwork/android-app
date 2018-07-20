package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.core.content.edit
import kotlinx.android.synthetic.main.fragment_withdrawal.*
import kotlinx.android.synthetic.main.layout_withdrawal_addr_bottom.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.maxDecimal
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.toDot
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.adapter.AddressAdapter
import one.mixin.android.vo.Address
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.support.v4.dip
import org.jetbrains.anko.textColor
import javax.inject.Inject

class WithdrawalFragment : BaseFragment() {

    companion object {
        const val TAG = "WithdrawalFragment"

        const val POS_ADD = 0
        const val POS_RV = 1

        fun newInstance(asset: AssetItem) = WithdrawalFragment().apply {
            val b = Bundle().apply {
                putParcelable(ARGS_ASSET, asset)
            }
            arguments = b
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }

    private val asset: AssetItem by lazy {
        arguments!!.getParcelable(ARGS_ASSET) as AssetItem
    }

    private var currAddr: Address? = null
    private val adapter: AddressAdapter by lazy { AddressAdapter(asset) }
    private var firstIn = true

    private val addrView: View by lazy {
        val view = View.inflate(context, R.layout.layout_withdrawal_addr_bottom, null)
        view.addr_book_title.left_ib.setOnClickListener { addrBottomSheet.dismiss() }
        view.addr_book_title.right_animator.setOnClickListener {
            activity?.addFragment(this@WithdrawalFragment,
                AddressManagementFragment.newInstance(asset), AddressManagementFragment.TAG)
            addrBottomSheet.dismiss()
        }
        view.addr_book_title.title_tv.text = getString(R.string.withdrawal_addr_book, asset.symbol)
        view.addr_rv.addItemDecoration(SpaceItemDecoration())
        view.addr_rv.adapter = adapter
        view.addr_add_tv.setOnClickListener {
            activity?.addFragment(this@WithdrawalFragment,
                AddressAddFragment.newInstance(asset), AddressAddFragment.TAG)
            addrBottomSheet.dismiss()
        }
        view
    }

    private val addrBottomSheet: BottomSheet by lazy {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomSheet = builder.create()
        builder.setCustomView(addrView)
        addrView.addr_book_title.left_ib.setOnClickListener { bottomSheet.dismiss() }
        bottomSheet
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_withdrawal, container, false)

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.setSubTitle(getString(R.string.withdrawal), asset.symbol)
        title_view.left_ib.setOnClickListener {
            if (amount_et.isFocused) amount_et.hideKeyboard()
            if (memo_et.isFocused) memo_et.hideKeyboard()
            activity?.onBackPressed()
        }
        title_view.right_animator.isEnabled = false
        title_view.right_animator.setOnClickListener {
            currAddr?.let {
                val amount = amount_et.text.toString()
                val withdrawalItem = WithdrawalBottomSheetDialogFragment.WithdrawalItem(if (noPublicKey(it)) it.accountTag!! else it.publicKey!!,
                    amount.toDot(), memo_et.text.toString(), it.addressId, if (noPublicKey(it)) it.accountName!! else it.label!!)
                val bottom = WithdrawalBottomSheetDialogFragment.newInstance(withdrawalItem, asset)
                bottom.setCallback(object : WithdrawalBottomSheetDialogFragment.Callback {
                    override fun onSuccess() {
                        fragmentManager?.popBackStackImmediate()
                    }
                })
                bottom.showNow(requireFragmentManager(), WithdrawalBottomSheetDialogFragment.TAG)
            }
            addrBottomSheet.dismiss()
        }
        balance_tv.text = "${getString(R.string.balance)} \n${asset.balance} ${asset.symbol}"
        addrBottomSheet.setCustomViewHeight(dip(300f))
        if (!asset.accountName.isNullOrEmpty()) {
            memo_rl.visibility = GONE
        }

        walletViewModel.addresses(asset.assetId).observe(this, Observer {
            if (it == null || it.isEmpty()) {
                addrView.addr_va.displayedChild = POS_ADD
                addr_tv.text = ""
                fee_tv.visibility = GONE
                title_view.right_tv.textColor = resources.getColor(R.color.text_gray, null)
                title_view.right_animator.isEnabled = false
            } else {
                addrView.addr_va.displayedChild = POS_RV
                val defaultAddrId = defaultSharedPreferences.getString(asset.assetId, null)
                val addr = if (defaultAddrId != null) {
                    var find: Address? = null
                    it.forEach { if (it.addressId == defaultAddrId) find = it }
                    if (find != null) find!! else it[0]
                } else {
                    it[0]
                }
                currAddr = addr
                if (firstIn) {
                    firstIn = false
                    walletViewModel.refreshAddressById(addr.addressId)
                }
                refreshFeeUI(addr)
                setAddrTv(addr)
            }
            adapter.addresses = it?.toMutableList()
        })
        amount_rl.setOnClickListener { amount_et.showKeyboard() }
        amount_et.addTextChangedListener(mWatcher)
        addr_rl.setOnClickListener {
            amount_et.hideKeyboard()
            context?.let {
                adapter.setAddrListener(object : AddressAdapter.SimpleAddressListener() {
                    override fun onAddrClick(addr: Address) {
                        currAddr = addr
                        walletViewModel.refreshAddressById(addr.addressId)
                        setAddrTv(addr)
                        adapter.notifyDataSetChanged()
                        addrBottomSheet.dismiss()
                    }
                })
                addrBottomSheet.show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setAddrTv(addr: Address) {
        addr_tv.text = if (noPublicKey(addr)) addr.accountName + " (" + addr.accountTag!!.formatPublicKey() + ")"
        else addr.label + " (" + if (addr.publicKey.isNullOrEmpty()) "" else addr.publicKey!!.formatPublicKey() + ")"
        defaultSharedPreferences.edit { putString(addr.assetId, addr.addressId) }
        if (canNext(amount_et.text)) {
            title_view.right_tv.textColor = resources.getColor(R.color.colorBlue, null)
            title_view.right_animator.isEnabled = true
        }
    }

    private fun noPublicKey(addr: Address) = !addr.accountName.isNullOrEmpty()

    private fun refreshFeeUI(addr: Address) {
        val bold = addr.fee + " " + asset.chainSymbol
        val str = try {
            val reserveDouble = addr.reserve.toDouble()
            if (reserveDouble > 0) {
                getString(R.string.withdrawal_fee_with_reserve, bold, asset.symbol, asset.name, addr.reserve, asset.symbol)
            } else {
                getString(R.string.withdrawal_fee, bold, asset.name)
            }
        } catch (e: NumberFormatException) {
            getString(R.string.withdrawal_fee, bold, asset.name)
        }
        val ssb = SpannableStringBuilder(str)
        val start = str.indexOf(bold)
        ssb.setSpan(StyleSpan(Typeface.BOLD), start,
            start + bold.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        fee_tv?.visibility = View.VISIBLE
        fee_tv?.text = ssb
    }

    private fun canNext(s: Editable) = s.isNotEmpty() && addr_tv.text.isNotEmpty()

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable) {
            s.maxDecimal()
            if (s.isNotEmpty()) {
                amount_et.textSize = 26f
            } else {
                amount_et.textSize = 16f
            }
            if (canNext(s)) {
                title_view.right_tv.textColor = resources.getColor(R.color.colorBlue, null)
                title_view.right_animator.isEnabled = true
            } else {
                title_view.right_tv.textColor = resources.getColor(R.color.text_gray, null)
                title_view.right_animator.isEnabled = false
            }
        }
    }
}