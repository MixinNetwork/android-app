package one.mixin.android.ui.wallet
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import one.mixin.android.R
import one.mixin.android.databinding.ViewWalletTransactionsSendBottomBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navigate
import one.mixin.android.extension.putString
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet

class SendBottomSheet(
    private val fragment: Fragment,
    @IdRes private val navContactAction: Int,
    @IdRes private val navAddressAction: Int,
    private val navTipAction: (() -> Unit)? = null,
) {
    private var _bottomSendBinding: ViewWalletTransactionsSendBottomBinding? = null
    private val bottomSendBinding get() = requireNotNull(_bottomSendBinding)

    fun show(asset: TokenItem) {
        val builder = BottomSheet.Builder(fragment.requireActivity())
        _bottomSendBinding =
            ViewWalletTransactionsSendBottomBinding.bind(
                View.inflate(
                    ContextThemeWrapper(fragment.requireActivity(), R.style.Custom),
                    R.layout.view_wallet_transactions_send_bottom,
                    null,
                ),
            )
        builder.setCustomView(bottomSendBinding.root)
        val bottomSheet = builder.create()
        bottomSendBinding.apply {
            contact.setOnClickListener {
                bottomSheet.dismiss()
                fragment.defaultSharedPreferences.putString(TransferFragment.ASSET_PREFERENCE, asset.assetId)
                this@SendBottomSheet.fragment.view?.navigate(
                    navContactAction,
                    Bundle().apply {
                        putParcelable(TransactionsFragment.ARGS_ASSET, asset)
                    },
                )
            }
            address.setOnClickListener {
                bottomSheet.dismiss()
                this@SendBottomSheet.fragment.view?.navigate(
                    navAddressAction,
                    Bundle().apply {
                        putParcelable(TransactionsFragment.ARGS_ASSET, asset)
                    },
                )
            }
            sendCancel.setOnClickListener { bottomSheet.dismiss() }
        }

        bottomSheet.show()
    }

    fun release() {
        _bottomSendBinding = null
    }
}
