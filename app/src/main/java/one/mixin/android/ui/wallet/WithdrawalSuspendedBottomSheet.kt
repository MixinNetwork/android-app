package one.mixin.android.ui.wallet
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import one.mixin.android.R
import one.mixin.android.databinding.WithdralBottomBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet

class WithdrawalSuspendedBottomSheet(
    private val fragment: Fragment,
) {
    private var _bottomSendBinding: WithdralBottomBinding? = null
    private val bottomSendBinding get() = requireNotNull(_bottomSendBinding)

    fun show(asset: TokenItem) {
        val builder = BottomSheet.Builder(fragment.requireActivity())
        _bottomSendBinding = WithdralBottomBinding.bind(
            View.inflate(
                ContextThemeWrapper(fragment.requireActivity(), R.style.Custom),
                R.layout.withdral_bottom,
                null,
            ),
        )
        builder.setCustomView(bottomSendBinding.root)
        val bottomSheet = builder.create()
        bottomSendBinding.apply {
            assetIcon.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            assetIcon.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            cancel.setOnClickListener { bottomSheet.dismiss() }
            title.text = fragment.getString(R.string.Withdrawal_Suspended, asset.symbol)
            content.text = fragment.getString(R.string.Withdrawal_Suspended_Content, asset.symbol)
            gotItTv.setOnClickListener { bottomSheet.dismiss() }
        }

        bottomSheet.show()
    }

    fun release() {
        _bottomSendBinding = null
    }
}
