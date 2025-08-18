package one.mixin.android.ui.address

import android.view.ContextThemeWrapper
import android.view.View
import androidx.fragment.app.Fragment
import one.mixin.android.R
import one.mixin.android.databinding.ViewReceiveSelectionBottomBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet

class ReceiveSelectionBottom(
    private val fragment: Fragment,
) {
    private var _bottomSendBinding: ViewReceiveSelectionBottomBinding? = null
    private val bottomSendBinding get() = requireNotNull(_bottomSendBinding)

    fun show(asset: TokenItem) {
        val builder = BottomSheet.Builder(fragment.requireActivity())
        _bottomSendBinding =
            ViewReceiveSelectionBottomBinding.bind(
                View.inflate(
                    ContextThemeWrapper(fragment.requireActivity(), R.style.Custom),
                    R.layout.view_receive_selection_bottom,
                    null,
                ),
            )
        builder.setCustomView(bottomSendBinding.root)
        val bottomSheet = builder.create()
        bottomSendBinding.apply {
            title.text = "${fragment.getString(R.string.Add)} ${asset.symbol}"
            wallet.setOnClickListener {
                _onReceiveSelectionClicker?.onWalletClick()
                bottomSheet.dismiss()
            }
            address.setOnClickListener {
                _onReceiveSelectionClicker?.onAddressClick()
                bottomSheet.dismiss()
            }
            cancel.setOnClickListener { bottomSheet.dismiss() }
        }

        bottomSheet.setOnDismissListener {
            release()
        }

        bottomSheet.show()
    }

    interface OnReceiveSelectionClicker {
        abstract fun onAddressClick()
        abstract fun onWalletClick()
    }

    private var _onReceiveSelectionClicker: OnReceiveSelectionClicker? = null

    fun setOnReceiveSelectionClicker(onReceiveSelectionClicker: OnReceiveSelectionClicker) {
        _onReceiveSelectionClicker = onReceiveSelectionClicker
    }

    fun release() {
        _bottomSendBinding = null
    }
}
