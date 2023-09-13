package one.mixin.android.ui.wallet.fiatmoney

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class OrderInfo(
    val number: String,
    val exchangeRate: String,
    val purchase: String,
    val feeByGateway: String,
    val feeByMixin: String,
    val purchaseTotal: String,
    val assetAmount: String,
) : Parcelable
