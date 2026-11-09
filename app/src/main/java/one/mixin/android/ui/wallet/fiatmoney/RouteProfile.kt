package one.mixin.android.ui.wallet.fiatmoney

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.ui.setting.Currency

@Parcelize
data class RouteProfile(
    var supportCurrencies: List<Currency>,
    var supportAssetIds: List<String>,
) : Parcelable
