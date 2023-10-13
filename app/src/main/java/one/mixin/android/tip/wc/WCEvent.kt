package one.mixin.android.tip.wc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.tip.wc.WalletConnect.RequestType

@Parcelize
sealed class WCEvent(
    open val version: WalletConnect.Version,
    open val requestType: RequestType,
) : Parcelable {
    @Parcelize
    data class V2(
        override val version: WalletConnect.Version,
        override val requestType: RequestType,
        val topic: String,
    ) : WCEvent(version, requestType)

    @Parcelize
    data class TIP(
        override val version: WalletConnect.Version,
        override val requestType: RequestType,
    ) : WCEvent(version, requestType)
}

class WCErrorEvent(val error: WCError)

@Parcelize
data class WCError(val throwable: Throwable) : Parcelable
