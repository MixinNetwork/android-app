package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.uber.autodispose.autoDispose
import kotlinx.android.synthetic.main.fragment_pin_bottom_sheet_address.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.notNullWithElse
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
        const val ARGS_DESTINATION = "args_destination"
        const val ARGS_TAG = "args_tag"
        const val ARGS_ADDRESS_ID = "args_address_id"
        const val ARGS_TYPE = "args_type"

        fun newInstance(
            assetId: String? = null,
            assetName: String? = null,
            assetUrl: String? = null,
            chainIconUrl: String? = null,
            label: String,
            destination: String,
            tag: String? = null,
            addressId: String? = null,
            type: Int = ADD
        ) = PinAddrBottomSheetDialogFragment().apply {
            val b = bundleOf(
                ARGS_ASSET_ID to assetId,
                ARGS_ASSET_NAME to assetName,
                ARGS_ASSET_URL to assetUrl,
                ARGS_CHAIN_URL to chainIconUrl,
                ARGS_LABEL to label,
                ARGS_DESTINATION to destination,
                ARGS_ADDRESS_ID to addressId,
                ARGS_TYPE to type,
                ARGS_TAG to tag
            )
            arguments = b
        }
    }

    private val assetId: String? by lazy { arguments!!.getString(ARGS_ASSET_ID) }
    private val assetName: String? by lazy { arguments!!.getString(ARGS_ASSET_NAME) }
    private val assetUrl: String? by lazy { arguments!!.getString(ARGS_ASSET_URL) }
    private val chainIconUrl: String? by lazy { arguments!!.getString(ARGS_CHAIN_URL) }
    private val label: String? by lazy { arguments!!.getString(ARGS_LABEL) }
    private val destination: String? by lazy { arguments!!.getString(ARGS_DESTINATION) }
    private val addressId: String? by lazy { arguments!!.getString(ARGS_ADDRESS_ID) }
    private val type: Int by lazy { arguments!!.getInt(ARGS_TYPE) }
    private val addressTag: String? by lazy { arguments!!.getString(ARGS_TAG) }

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
        contentView.asset_name.text = label
        contentView.asset_address.text = destination


        contentView.pin.setListener(object : PinView.OnPinListener {
            override fun onUpdate(index: Int) {
                if (index != contentView.pin.getCount()) return

                contentView.pin_va?.displayedChild = POS_PB
                val observable = if (type == ADD || type == MODIFY) {
                    bottomViewModel.syncAddr(assetId!!, destination, label, addressTag, contentView.pin.code())
                } else {
                    bottomViewModel.deleteAddr(addressId!!, contentView.pin.code())
                }
                observable.autoDispose(stopScope).subscribe({ r ->
                    if (r.isSuccess) {
                        doAsync {
                            if (type == ADD || type == MODIFY) {
                                bottomViewModel.saveAddr(r.data as Address)
                            } else {
                                bottomViewModel.deleteLocalAddr(addressId!!)
                            }

                            uiThread {
                                contentView.pin_va?.displayedChild = POS_PIN
                                callback.notNullWithElse({ action -> action.onSuccess() }, {
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
                        if (r.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                            toast(R.string.error_pin_check_too_many_request)
                        } else {
                            ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                        }
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
