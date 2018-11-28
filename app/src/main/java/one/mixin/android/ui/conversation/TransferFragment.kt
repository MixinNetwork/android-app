package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkManager
import com.uber.autodispose.kotlin.autoDisposable
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
import one.mixin.android.extension.maxDecimal
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.putString
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.toDot
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.ui.common.BiometricDialog
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.BiometricUtil.REQUEST_CODE_CREDENTIALS
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import one.mixin.android.work.RefreshAssetsWorker
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
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

    private val adapter by lazy {
        TypeAdapter()
    }

    private val userId: String by lazy { arguments!!.getString(ARGS_USER_ID) }

    private var user: User? = null

    private val assetsView: View by lazy {
        val view = View.inflate(context, R.layout.view_wallet_transfer_type_bottom, null)
        view.type_rv.addItemDecoration(SpaceItemDecoration())
        view.type_rv.adapter = adapter
        view
    }

    private val assetsBottomSheet: BottomSheet by lazy {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomSheet = builder.create()
        builder.setCustomView(assetsView)
        bottomSheet.setOnDismissListener {
            if (isAdded) {
                contentView.transfer_amount.post { contentView.transfer_amount.showKeyboard() }
            }
        }
        bottomSheet
    }

    private var biometricDialog: BiometricDialog? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshAssetsWorker>()
        contentView.title_view.left_ib.setOnClickListener { dismiss() }
        contentView.title_view.avatar_iv.visibility = View.VISIBLE
        contentView.title_view.avatar_iv.setTextSize(16f)
        contentView.transfer_amount.addTextChangedListener(mWatcher)
        contentView.asset_rl.setOnClickListener {
            contentView.transfer_amount.hideKeyboard()
            context?.let {
                adapter.coins = assets
                adapter.setTypeListener(object : OnTypeClickListener {
                    override fun onTypeClick(asset: AssetItem) {
                        currentAsset = asset
                        contentView.asset_name.text = asset.name
                        contentView.asset_desc.text = asset.balance.numberFormat()
                        contentView.asset_avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                        contentView.asset_avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
                        adapter.notifyDataSetChanged()
                        assetsBottomSheet.dismiss()
                    }
                })

                assetsView.type_cancel.setOnClickListener {
                    assetsBottomSheet.dismiss()
                }
                assetsBottomSheet.show()

                if (assets.size > 3) {
                    assetsBottomSheet.setCustomViewHeight(it.dpToPx(300f))
                }
            }
        }

        chatViewModel.findUserById(userId).observe(this, Observer { u ->
            if (u == null) {
                jobManager.addJobInBackground(RefreshUserJob(listOf(userId)))
            } else {
                user = u

                contentView.title_view.setSubTitle(getString(R.string.conversation_status_transfer), getString(R.string.to, u.fullName))
                contentView.title_view.avatar_iv.setInfo(u.fullName, u.avatarUrl, u.identityNumber)
            }
        })

        contentView.continue_animator.setOnClickListener {
            if (!isAdded || user == null) return@setOnClickListener

            contentView.transfer_amount.hideKeyboard()

            if (BiometricUtil.shouldShowBiometric(requireContext())) {
                showBiometricPrompt()
            } else {
                showTransferBottom()
            }
        }

        chatViewModel.assetItemsWithBalance().observe(this, Observer { r: List<AssetItem>? ->
            if (r != null && r.isNotEmpty()) {
                assets = r
                adapter.coins = r
                contentView.expand_iv.visibility = VISIBLE
                contentView.asset_rl.isEnabled = true

                notNullElse(r.find {
                    it.assetId == activity?.defaultSharedPreferences!!.getString(ASSERT_PREFERENCE, "")
                }, { a ->
                    contentView.asset_avatar.bg.loadImage(a.iconUrl, R.drawable.ic_avatar_place_holder)
                    contentView.asset_avatar.badge.loadImage(a.chainIconUrl, R.drawable.ic_avatar_place_holder)
                    contentView.asset_name.text = a.name
                    contentView.asset_desc.text = a.balance.numberFormat()
                    currentAsset = a
                }, {
                    val a = assets[0]
                    contentView.asset_avatar.bg.loadImage(a.iconUrl, R.drawable.ic_avatar_place_holder)
                    contentView.asset_avatar.badge.loadImage(a.chainIconUrl, R.drawable.ic_avatar_place_holder)
                    contentView.asset_name.text = a.name
                    contentView.asset_desc.text = a.balance.numberFormat()
                    currentAsset = a
                })
            } else {
                contentView.expand_iv.visibility = GONE
                contentView.asset_rl.isEnabled = false

                doAsync {
                    val xin = chatViewModel.getXIN()
                    uiThread {
                        if (!isAdded) return@uiThread

                        notNullElse(xin, {
                            contentView.asset_avatar.bg.loadImage(it.iconUrl, R.drawable.ic_avatar_place_holder)
                            contentView.asset_avatar.badge.loadImage(it.chainIconUrl, R.drawable.ic_avatar_place_holder)
                            contentView.asset_name.text = it.name
                            contentView.asset_desc.text = it.balance.numberFormat()
                        }, {
                            contentView.asset_avatar.bg.setImageResource(R.drawable.ic_avatar_place_holder)
                            contentView.asset_name.text = getString(R.string.app_name)
                            contentView.asset_desc.text = "0"
                        })
                    }
                }
            }
        })

        contentView.transfer_amount.post { contentView.transfer_amount.showKeyboard() }
    }

    private fun showBiometricPrompt() {
        biometricDialog = BiometricDialog(requireContext(), user!!, contentView.transfer_amount.text.toString().toDot(),
            currentAsset!!.toAsset(), UUID.randomUUID().toString(), contentView.transfer_memo.text.toString())
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    private fun showTransferBottom() {
        val bottom = TransferBottomSheetDialogFragment
            .newInstance(user!!, contentView.transfer_amount.text.toString().toDot(), currentAsset!!.toAsset(), UUID.randomUUID().toString(),
                contentView.transfer_memo.text.toString())
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

    private val mWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable) {
            s.maxDecimal()
            if (s.isNotEmpty() && contentView.asset_rl.isEnabled && s.toString().checkNumber()) {
                contentView.transfer_amount.textSize = 26f
                contentView.continue_animator.visibility = VISIBLE
            } else {
                contentView.transfer_amount.textSize = 16f
                contentView.continue_animator.visibility = GONE
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
            chatViewModel.transfer(assetId, userId, amount, pin, trace, memo).autoDisposable(scopeProvider)
                .subscribe({
                    if (it.isSuccess) {
                        dialog?.dismiss()
                    } else {
                        ErrorHandler.handleMixinError(it.errorCode)
                    }
                }, {
                    ErrorHandler.handleError(it)
                })
        }

        override fun showTransferBottom(user: User, amount: String, asset: Asset, trace: String?, memo: String?) {
            showTransferBottom()
        }

        override fun showAuthenticationScreen() {
            BiometricUtil.showAuthenticationScreen(this@TransferFragment)
        }

        override fun onCancel() {}
    }

    class TypeAdapter : RecyclerView.Adapter<ItemHolder>() {
        var coins: List<AssetItem>? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        private var typeListener: OnTypeClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_transfer_type, parent, false))

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            if (coins == null || coins!!.isEmpty()) {
                return
            }
            val itemAssert = coins!![position]
            holder.itemView.type_avatar.bg.loadImage(itemAssert.iconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.type_avatar.badge.loadImage(itemAssert.chainIconUrl, R.drawable.ic_avatar_place_holder)
            holder.itemView.name.text = itemAssert.name
            holder.itemView.value.text = itemAssert.balance.numberFormat()
            currentAsset?.let {
                holder.itemView.check_iv.visibility = if (itemAssert.assetId == currentAsset?.assetId) VISIBLE else GONE
            }
            holder.itemView.setOnClickListener {
                typeListener?.onTypeClick(itemAssert)
            }
        }

        override fun getItemCount(): Int = notNullElse(coins, { it.size }, 0)

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
