package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.security.keystore.UserNotAuthenticatedException
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.biometrics.BiometricPrompt
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_transfer.view.*
import kotlinx.android.synthetic.main.item_transfer_type.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_wallet_transfer_type_bottom.view.*
import one.mixin.android.Constants
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.R
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.maxDecimal
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.putString
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.toDot
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.wallet.BiometricTimeFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.BiometricUtil.REQUEST_CODE_CREDENTIALS
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.math.BigDecimal
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.Executors
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        jobManager.addJobInBackground(RefreshAssetsJob())
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
        val biometricPrompt = BiometricPrompt(requireActivity(), HandlerExecutor(Handler(Looper.getMainLooper())), biometricCallback)
        val biometricPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.wallet_bottom_transfer_to, user!!.fullName))
            .setSubtitle(getString(R.string.contact_mixin_id, user!!.identityNumber))
            .setDescription(getDescription())
            .setNegativeButtonText(getString(R.string.wallet_pay_with_pwd))
            .build()
        val cipher = try {
            BiometricUtil.getDecryptCipher(requireContext())
        } catch (e: UserNotAuthenticatedException) {
            BiometricUtil.showAuthenticationScreen(this)
            return
        }
        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        biometricPrompt.authenticate(biometricPromptInfo, cryptoObject)
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

    private fun startTransfer(pin: String) {
        chatViewModel.transfer(currentAsset!!.assetId, user!!.userId, contentView.transfer_amount.text.toString().toDot(),
            pin, UUID.randomUUID().toString(), contentView.transfer_memo.text.toString()).autoDisposable(scopeProvider)
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

    private fun getDescription(): String {
        val amount = contentView.transfer_amount.text.toString()
        val pre = "$amount ${currentAsset!!.symbol}"
        val post = getString(R.string.wallet_unit_usd,
            "≈ ${(BigDecimal(contentView.transfer_amount.text.toString().toDot()) * BigDecimal(currentAsset!!.priceUsd)).numberFormat2()}")
        return "$pre ($post)"
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
            if (s.isNotEmpty() && contentView.asset_rl.isEnabled) {
                contentView.transfer_amount.textSize = 26f
                contentView.continue_animator.visibility = VISIBLE
            } else {
                contentView.transfer_amount.textSize = 16f
                contentView.continue_animator.visibility = GONE
            }
        }
    }

    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            val cipher = result.cryptoObject?.cipher
            if (cipher != null) {
                try {
                    val encrypt = defaultSharedPreferences.getString(Constants.BIOMETRICS_PIN, null)
                    val decryptByteArray = cipher.doFinal(Base64.decode(encrypt, Base64.URL_SAFE))
                    startTransfer(decryptByteArray.toString(Charset.defaultCharset()))
                } catch (e: Exception) {
                }
            }
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            showTransferBottom()
        }
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
