package one.mixin.android.event

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment

@Parcelize
sealed class WCEvent(
    open val version: WalletConnect.Version,
    open val requestType: WalletConnectBottomSheetDialogFragment.RequestType,
) : Parcelable {

    @Parcelize
    data class V1(
        override val version: WalletConnect.Version,
        override val requestType: WalletConnectBottomSheetDialogFragment.RequestType,
        val id: Long,
    ) : WCEvent(version, requestType)

    @Parcelize
    data class V2(
        override val version: WalletConnect.Version,
        override val requestType: WalletConnectBottomSheetDialogFragment.RequestType,
    ) : WCEvent(version, requestType)
}
