package one.mixin.android.ui.panel

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_panel_transfer.view.*
import kotlinx.android.synthetic.main.layout_panel_transfer_asset.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.R
import one.mixin.android.extension.checkNumber
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.maxDecimal
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.putString
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.toDot
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BiometricDialog
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.panel.adapter.PanelTransferAssetAdapter
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.BiometricUtil.REQUEST_CODE_CREDENTIALS
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Asset
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView
import one.mixin.android.widget.keyboard.KeyboardAwareLinearLayout
import one.mixin.android.worker.RefreshAssetsWorker
import one.mixin.android.worker.RefreshUserWorker
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.UUID
import javax.inject.Inject

@SuppressLint("InflateParams")
class PanelTransferFragment : PanelBottomSheet(), KeyboardAwareLinearLayout.OnKeyboardHiddenListener, KeyboardAwareLinearLayout.OnKeyboardShownListener {

    companion object {
        const val TAG = "PanelTransferFragment"
        const val ASSERT_PREFERENCE = "TRANSFER_ASSERT"

        fun newInstance(userId: String) = PanelTransferFragment().apply {
            arguments = bundleOf(
                ARGS_USER_ID to userId
            )
        }
    }

    override fun getContentViewId() = R.layout.fragment_panel_transfer

    override fun onTapPanelBar() {
        contentView.transfer_amount.hideKeyboard()
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
            activity?.defaultSharedPreferences!!.putString(ASSERT_PREFERENCE, value?.assetId)
        }

    private val userId: String by lazy { arguments!!.getString(ARGS_USER_ID) }

    private var user: User? = null

    private var biometricDialog: BiometricDialog? = null

    private var assetsAdapter = PanelTransferAssetAdapter()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshAssetsWorker>()
        contentView.transfer_amount.addTextChangedListener(mWatcher)
        contentView.asset_rl.setOnClickListener {
            contentView.transfer_amount.hideKeyboard()
            contentView.asset_layout.animate().translationX(0f).start()
        }

        chatViewModel.findUserById(userId).observe(this, Observer { u ->
            if (u == null) {
                WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshUserWorker>(
                    workDataOf(RefreshUserWorker.USER_IDS to arrayOf(userId)))
            } else {
                user = u
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
                assetsAdapter.submitList(r)
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
                assetsAdapter.currentAsset = currentAsset
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

        contentView.back_iv.setOnClickListener {
            contentView.search_et.hideKeyboard()
            contentView.asset_layout.animate().translationX(contentView.asset_layout.width.toFloat()).start()
        }
        contentView.assets_rv.layoutManager = LinearLayoutManager(requireContext())
        contentView.assets_rv.adapter = assetsAdapter
        assetsAdapter.onAssetListener = object : PanelTransferAssetAdapter.OnAssetListener {
            override fun onItemClick(assetItem: AssetItem) {
                currentAsset = assetItem
                assetsAdapter.currentAsset = assetItem
                contentView.asset_name.text = assetItem.name
                contentView.asset_desc.text = assetItem.balance.numberFormat()
                contentView.asset_avatar.bg.loadImage(assetItem.iconUrl, R.drawable.ic_avatar_place_holder)
                contentView.asset_avatar.badge.loadImage(assetItem.chainIconUrl, R.drawable.ic_avatar_place_holder)
                contentView.search_et.text.clear()
                contentView.asset_layout.animate().translationX(contentView.asset_layout.width.toFloat()).start()
            }
        }
        contentView.search_et.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
                assetsAdapter.submitList(if (s.isNullOrBlank()) {
                    assets
                } else {
                    assets.filter { it.name.startsWith(s, true) }
                })
            }

            override fun onSearch() {
                // Left empty for local data filter
            }
        }

        (dialog as BottomSheet).setCustomViewHeight(maxHeight)
        contentView.transfer_amount.post { contentView.transfer_amount.showKeyboard() }
    }

    override fun onResume() {
        super.onResume()
        contentView.input_layout.addOnKeyboardShownListener(this)
        contentView.input_layout.addOnKeyboardHiddenListener(this)
    }

    override fun onPause() {
        super.onPause()
        contentView.input_layout.removeOnKeyboardShownListener(this)
        contentView.input_layout.removeOnKeyboardHiddenListener(this)
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

    override fun onKeyboardShown() {
        updateContinueAnimatorBottomMargin(true)
    }

    override fun onKeyboardHidden() {
        updateContinueAnimatorBottomMargin(false)
    }

    private fun updateContinueAnimatorBottomMargin(shown: Boolean) {
        contentView.continue_animator.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = if (shown) {
                contentView.input_layout.keyboardHeight
            } else {
                0
            }
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
            BiometricUtil.showAuthenticationScreen(this@PanelTransferFragment)
        }

        override fun onCancel() {}
    }
}
