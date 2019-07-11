package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.uber.autodispose.autoDisposable
import kotlinx.android.synthetic.main.fragment_pin_bottom_sheet_address.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.PinBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Address
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.PinView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class PinAddrBottomSheetDialogFragment : PinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "PinAddrBottomSheetDialogFragment"

        const val ADD = 0
        const val DELETE = 1
        const val MODIFY = 2

        const val ARGS_ASSET_ID = "args_asset_id"
        const val ARGS_ASSET_NAME = "args_asset_name"
        const val ARGS_ASSET_URL = "args_asset_url"
        const val ARGS_CHAIN_URL = "args_chain_url"
        const val ARGS_LABEL = "args_label"
        const val ARGS_PUBLIC_KEY = "args_public_key"
        const val ARGS_ADDRESS_ID = "args_address_id"
        const val ARGS_TYPE = "args_type"
        const val ARGS_ACCOUNT_NAME = "args_account_name"
        const val ARGS_ACCOUNT_TAG = "args_account_tag"

        fun newInstance(
            assetId: String? = null,
            assetName: String? = null,
            assetUrl: String? = null,
            chainIconUrl: String? = null,
            label: String? = null,
            publicKey: String? = null,
            addressId: String? = null,
            type: Int = ADD,
            accountName: String? = null,
            accountTag: String? = null
        ) = PinAddrBottomSheetDialogFragment().apply {
            val b = bundleOf(
                ARGS_ASSET_ID to assetId,
                ARGS_ASSET_NAME to assetName,
                ARGS_ASSET_URL to assetUrl,
                ARGS_CHAIN_URL to chainIconUrl,
                ARGS_LABEL to label,
                ARGS_PUBLIC_KEY to publicKey,
                ARGS_ADDRESS_ID to addressId,
                ARGS_TYPE to type,
                ARGS_ACCOUNT_NAME to accountName,
                ARGS_ACCOUNT_TAG to accountTag
            )
            arguments = b
        }
    }

    private val assetId: String? by lazy { arguments!!.getString(ARGS_ASSET_ID) }
    private val assetName: String? by lazy { arguments!!.getString(ARGS_ASSET_NAME) }
    private val assetUrl: String? by lazy { arguments!!.getString(ARGS_ASSET_URL) }
    private val chainIconUrl: String? by lazy { arguments!!.getString(ARGS_CHAIN_URL) }
    private val label: String? by lazy { arguments!!.getString(ARGS_LABEL) }
    private val publicKey: String? by lazy { arguments!!.getString(ARGS_PUBLIC_KEY) }
    private val addressId: String? by lazy { arguments!!.getString(ARGS_ADDRESS_ID) }
    private val type: Int by lazy { arguments!!.getInt(ARGS_TYPE) }
    private val accountName: String? by lazy { arguments!!.getString(ARGS_ACCOUNT_NAME) }
    private val accountTag: String? by lazy { arguments!!.getString(ARGS_ACCOUNT_TAG) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_pin_bottom_sheet_address, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.info_tv.setText(getTipTextRes())
        contentView.title_view.right_iv.setOnClickListener { dismiss() }
        contentView.title_view.setSubTitle(getString(when (type) {
            ADD -> R.string.withdrawal_addr_add
            MODIFY -> R.string.withdrawal_addr_modify
            else -> R.string.withdrawal_addr_delete
        }, assetName))
        contentView.asset_icon.bg.loadImage(assetUrl, R.drawable.ic_avatar_place_holder)
        contentView.asset_icon.badge.loadImage(chainIconUrl, R.drawable.ic_avatar_place_holder)
        contentView.asset_name.text = if (!accountName.isNullOrBlank()) {
            accountName
        } else {
            label
        }
        contentView.asset_address.text = if (!accountTag.isNullOrBlank()) {
            accountTag
        } else {
            publicKey
        }

        contentView.pin.setListener(object : PinView.OnPinListener {
            override fun onUpdate(index: Int) {
                if (index != contentView.pin.getCount()) return

                contentView.pin_va?.displayedChild = POS_PB
                val observable = if (type == ADD || type == MODIFY) {
                    bottomViewModel.syncAddr(assetId!!, publicKey, label, contentView.pin.code(), accountName, accountTag)
                } else {
                    bottomViewModel.deleteAddr(addressId!!, contentView.pin.code())
                }
                observable.autoDisposable(stopScope).subscribe({ r ->
                    if (r.isSuccess) {
                        doAsync {
                            if (type == ADD || type == MODIFY) {
                                bottomViewModel.saveAddr(r.data as Address)
                            } else {
                                bottomViewModel.deleteLocalAddr(addressId!!)
                            }

                            uiThread {
                                contentView.pin_va?.displayedChild = POS_PIN
                                callback.notNullElse({ action -> action.onSuccess() }, {
                                    toast(R.string.successful)
                                })
                                dismiss()
                            }
                        }
                    } else {
                        if (r.errorCode != ErrorHandler.PIN_INCORRECT) {
                            dismiss()
                        } else {
                            contentView.pin_va?.displayedChild = POS_PIN
                            contentView.pin?.clear()
                        }
                        ErrorHandler.handleMixinError(r.errorCode)
                    }
                }, { t ->
                    contentView.pin_va?.displayedChild = POS_PIN
                    contentView.pin.clear()
                    ErrorHandler.handleError(t)
                })
            }
        })
    }

    private fun getTipTextRes(): Int = when (type) {
        ADD -> R.string.withdrawal_addr_pin_add
        DELETE -> R.string.withdrawal_addr_pin_delete
        MODIFY -> R.string.withdrawal_addr_pin_modify
        else -> R.string.withdrawal_addr_pin_add
    }
}