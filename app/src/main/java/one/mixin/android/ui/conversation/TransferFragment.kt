package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkManager
import kotlinx.android.synthetic.main.fragment_transfer.view.*
import kotlinx.android.synthetic.main.item_transfer_type.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_wallet_transfer_type_bottom.view.*
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.R
import one.mixin.android.extension.checkNumber
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.putString
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.ui.common.BiometricDialog
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.BiometricUtil.REQUEST_CODE_CREDENTIALS
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView
import one.mixin.android.widget.getMaxCustomViewHeight
import one.mixin.android.worker.RefreshAssetsWorker
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.textColor
import org.jetbrains.anko.uiThread
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject

@SuppressLint("InflateParams")
class TransferFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "TransferFragment"
        const val ASSERT_PREFERENCE = "TRANSFER_ASSERT"

        fun newInstance(userId: String) = TransferFragment().apply {
            arguments = bundleOf(
                ARGS_USER_ID to userId
            )
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_transfer, null)
        contentView.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        (dialog as BottomSheet).apply {
            fullScreen = true
            setCustomView(contentView)

            onDismissListener = object : OnDismissListener {
                override fun onDismiss() {
                    if (isAdded) {
                        operateKeyboard(false)
                    }
                }
            }
        }
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    private val chatViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private var assets = listOf<AssetItem>()
    private var currentAsset: AssetItem? = null
        set(value) {
            field = value
            adapter.currentAsset = value
            activity?.defaultSharedPreferences!!.putString(ASSERT_PREFERENCE, value?.assetId)
        }

    private val adapter = TypeAdapter()

    private val userId: String by lazy { arguments!!.getString(ARGS_USER_ID) }

    private var user: User? = null

    private var swaped = false
    private var bottomValue = 0.0

    private val assetsView: View by lazy {
        val view = View.inflate(context, R.layout.view_wallet_transfer_type_bottom, null)
        view.type_rv.adapter = adapter
        view
    }

    private val assetsBottomSheet: BottomSheet by lazy {
        val builder = BottomSheet.Builder(requireActivity(), true)
        val bottomSheet = builder.create()
        builder.setCustomView(assetsView)
        bottomSheet.setOnDismissListener {
            if (isAdded) {
                assetsView.search_et.text.clear()
                operateKeyboard(true)
            }
        }
        bottomSheet
    }

    private var biometricDialog: BiometricDialog? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshAssetsWorker>()
        contentView.title_view.left_ib.setOnClickListener { dismiss() }
        contentView.amount_et.addTextChangedListener(mWatcher)
        contentView.amount_et.filters = arrayOf(inputFilter)
        contentView.amount_rl.setOnClickListener { operateKeyboard(true) }
        contentView.asset_rl.setOnClickListener {
            operateKeyboard(false)
            context?.let {
                adapter.submitList(assets)
                adapter.setTypeListener(object : OnTypeClickListener {
                    override fun onTypeClick(asset: AssetItem) {
                        currentAsset = asset
                        updateAssetUI(asset)
                        adapter.notifyDataSetChanged()
                        assetsBottomSheet.dismiss()
                    }
                })

                assetsView.type_cancel.setOnClickListener {
                    assetsBottomSheet.dismiss()
                }
                assetsBottomSheet.show()
                assetsView.search_et.remainFocusable()
            }

            assetsBottomSheet.setCustomViewHeight(assetsBottomSheet.getMaxCustomViewHeight())
        }
        contentView.swap_iv.setOnClickListener {
            currentAsset?.let {
                swaped = !swaped
                updateAssetUI(it)
            }
        }
        assetsView.search_et.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
                filter(s.toString())
            }

            override fun onSearch() {
            }
        }

        chatViewModel.findUserById(userId).observe(this, Observer { u ->
            if (u == null) {
                jobManager.addJobInBackground(RefreshUserJob(listOf(userId)))
            } else {
                user = u
                contentView.avatar.setInfo(u.fullName, u.avatarUrl, u.userId)
                contentView.to_tv.text = getString(R.string.to, u.fullName)
            }
        })

        contentView.continue_animator.setOnClickListener {
            if (!isAdded || user == null) return@setOnClickListener

            operateKeyboard(false)

            if (BiometricUtil.shouldShowBiometric(requireContext())) {
                showBiometricPrompt()
            } else {
                showTransferBottom()
            }
        }

        chatViewModel.assetItemsWithBalance().observe(this, Observer { r: List<AssetItem>? ->
            if (r != null && r.isNotEmpty()) {
                assets = r
                adapter.submitList(r)
                contentView.asset_rl.isEnabled = true

                notNullElse(r.find {
                    it.assetId == activity?.defaultSharedPreferences!!.getString(ASSERT_PREFERENCE, "")
                }, { a ->
                    updateAssetUI(a)
                    currentAsset = a
                }, {
                    val a = assets[0]
                    updateAssetUI(a)
                    currentAsset = a
                })
            } else {
                contentView.asset_rl.isEnabled = false

                doAsync {
                    val xin = chatViewModel.getXIN()
                    uiThread {
                        if (!isAdded) return@uiThread

                        notNullElse(xin, {
                            updateAssetUI(it)
                        }, {
                            contentView.asset_avatar.bg.setImageResource(R.drawable.ic_avatar_place_holder)
                            contentView.asset_name.text = getString(R.string.app_name)
                            contentView.asset_desc.text = "0"
                        })
                    }
                }
            }
        })
    }

    private fun filter(s: String) {
        val assetList = assets.filter {
            it.name.contains(s, true) || it.symbol.contains(s, true)
        }
        adapter.submitList(assetList)
    }

    @SuppressLint("SetTextI18n")
    private fun updateAssetUI(asset: AssetItem) {
        val valuable = try {
            asset.priceUsd.toFloat() > 0f
        } catch (e: NumberFormatException) {
            false
        }
        if (valuable) {
            contentView.swap_iv.visibility = VISIBLE
        } else {
            swaped = false
            contentView.swap_iv.visibility = GONE
        }
        checkInputForbidden(contentView.amount_et.text)
        if (contentView.amount_et.text.isNullOrEmpty()) {
            if (swaped) {
                contentView.amount_et.hint = "0.00 USD"
                contentView.amount_as_tv.text = "0.00 ${asset.symbol}"
            } else {
                contentView.amount_et.hint = "0.00 ${asset.symbol}"
                contentView.amount_as_tv.text = "0.00 USD"
            }
            contentView.symbol_tv.text = ""
        } else {
            contentView.amount_et.hint = ""
            contentView.symbol_tv.text = getTopSymbol()
            contentView.amount_as_tv.text = getBottomText()
        }

        contentView.asset_name.text = asset.name
        contentView.asset_desc.text = asset.balance.numberFormat()
        contentView.asset_avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        contentView.asset_avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)

        operateKeyboard(true)
    }

    private fun getAmountView() = contentView.amount_et

    private fun getAmount(): String {
        return if (swaped) {
            bottomValue.toString()
        } else {
            val s = contentView.amount_et.text.toString()
            val symbol = if (swaped) currentAsset?.symbol ?: "" else "USD"
            val index = s.indexOf(symbol)
            return if (index != -1) {
                s.substring(0, index)
            } else {
                s
            }.trim()
        }
    }

    private fun getTopSymbol(): String {
        return if (swaped) "USD" else currentAsset?.symbol ?: ""
    }

    private fun getBottomText(): String {
        val amount = try {
            contentView.amount_et.text.toString().toDouble()
        } catch (e: java.lang.NumberFormatException) {
            0.0
        }
        val rightSymbol = if (swaped) currentAsset!!.symbol else "USD"
        val value = try {
            if (currentAsset == null || currentAsset!!.priceUsd.toDouble() == 0.0) {
                BigDecimal(0)
            } else if (swaped) {
                BigDecimal(amount).divide(BigDecimal(currentAsset!!.priceUsd), 8, RoundingMode.HALF_UP)
            } else {
                (BigDecimal(amount) * BigDecimal(currentAsset!!.priceUsd))
            }
        } catch (e: ArithmeticException) {
            BigDecimal(0)
        } catch (e: NumberFormatException) {
            BigDecimal(0)
        }
        bottomValue = value.toDouble()
        return "${if(swaped) {
            value.numberFormat8()
        } else value.numberFormat2()} $rightSymbol"
    }

    private fun operateKeyboard(show: Boolean) {
        val target = getAmountView()
        target.post {
            if (show) {
                target.showKeyboard()
            } else {
                target.hideKeyboard()
            }
        }
    }

    private fun showBiometricPrompt() {
        biometricDialog = BiometricDialog(requireContext(), user!!, getAmount(),
            currentAsset!!.toAsset(), UUID.randomUUID().toString(), contentView.transfer_memo.text.toString())
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    private fun showTransferBottom(trace: String? = null, pin: String? = null) {
        val bottom = TransferBottomSheetDialogFragment
            .newInstance(user!!, getAmount(), currentAsset!!.toAsset(), trace ?: UUID.randomUUID().toString(),
                contentView.transfer_memo.text.toString(), pin)
        bottom.showNow(requireFragmentManager(), TransferBottomSheetDialogFragment.TAG)
        bottom.setCallback(object : TransferBottomSheetDialogFragment.Callback {
            override fun onSuccess() {
                dialog?.dismiss()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CREDENTIALS && resultCode == RESULT_OK) {
            showBiometricPrompt()
        }
    }

    private val inputFilter = InputFilter { source, _, _, _, _, _ ->
        val s =  if (forbiddenInput and !ignoreFilter) "" else source
        ignoreFilter = false
        return@InputFilter s
    }

    private var forbiddenInput = false
    private var ignoreFilter = false

    private fun checkInputForbidden(s: CharSequence) {
        val index = s.indexOf('.')
        forbiddenInput = if (index == -1) {
            false
        } else {
            val num = s.split('.')
            val tail = num[1]
            if (swaped) {
                val tailLen = tail.length
                if (tailLen > 2) {
                    ignoreFilter = true
                    contentView.amount_et.setText(s.subSequence(0, num[0].length + 3))
                }
                tailLen >= 2
            } else {
                tail.length >= 8
            }
        }
    }

    private val mWatcher: TextWatcher = object : TextWatcher {

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        @SuppressLint("SetTextI18n")
        override fun afterTextChanged(s: Editable) {
            checkInputForbidden(s)
            if (s.isNotEmpty() && contentView.asset_rl.isEnabled && s.toString().checkNumber()) {
                contentView.continue_animator.background = resources.getDrawable(R.drawable.bg_wallet_blue_btn, null)
                contentView.continue_animator.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = requireContext().dpToPx(72f)
                    topMargin = requireContext().dpToPx(32f)
                    bottomMargin = 0
                }
                contentView.continue_tv.textColor = requireContext().getColor(R.color.white)
                if (contentView.amount_rl.isVisible && currentAsset != null) {
                    contentView.amount_et.hint = ""
                    contentView.symbol_tv.text = getTopSymbol()
                    contentView.amount_as_tv.text = getBottomText()
                }
            } else {
                contentView.continue_animator.background = resources.getDrawable(R.drawable.bg_gray_btn, null)
                contentView.continue_animator.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = requireContext().dpToPx(40f)
                    topMargin = requireContext().dpToPx(50f)
                    bottomMargin = requireContext().dpToPx(16f)
                }
                contentView.continue_tv.textColor = requireContext().getColor(R.color.wallet_text_gray)
                if (contentView.amount_rl.isVisible) {
                    contentView.amount_et.hint = "0.00 ${if (swaped) "USD" else currentAsset?.symbol}"
                    contentView.symbol_tv.text = ""
                    contentView.amount_as_tv.text = "0.00 ${if (swaped) currentAsset?.symbol else "USD"}"
                }
            }
        }
    }

    private val biometricDialogCallback = object : BiometricDialog.Callback {
        override fun onStartTransfer(
            assetId: String,
            userId: String,
            amount: String,
            pin: String,
            trace: String?,
            memo: String?
        ) {
            showTransferBottom(trace, pin)
        }

        override fun showTransferBottom(user: User, amount: String, asset: Asset, trace: String?, memo: String?) {
            showTransferBottom()
        }

        override fun showAuthenticationScreen() {
            BiometricUtil.showAuthenticationScreen(this@TransferFragment)
        }

        override fun onCancel() {}
    }

    class TypeAdapter : ListAdapter<AssetItem, ItemHolder>(AssetItem.DIFF_CALLBACK) {
        private var typeListener: OnTypeClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_transfer_type, parent, false))

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val itemAssert = getItem(position)
            holder.itemView.type_avatar.bg.loadImage(itemAssert.iconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.type_avatar.badge.loadImage(itemAssert.chainIconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.name.text = itemAssert.name
            holder.itemView.value.text = itemAssert.balance.numberFormat()
            currentAsset?.let {
                holder.itemView.check_iv.visibility = if (itemAssert.assetId == currentAsset?.assetId) VISIBLE else INVISIBLE
            }
            holder.itemView.setOnClickListener {
                typeListener?.onTypeClick(itemAssert)
            }
        }

        fun setTypeListener(listener: OnTypeClickListener) {
            typeListener = listener
        }

        var currentAsset: AssetItem? = null
    }

    interface OnTypeClickListener {
        fun onTypeClick(asset: AssetItem)
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
