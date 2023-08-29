package one.mixin.android.ui.wallet.fiatmoney

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class OrderInfo(
    val number: String,
    val price: String,
    val purchase: String,
    val fee: String,
    val total: String,
    val feePercent: String,
    val assetAmount: String,
) : Parcelable
