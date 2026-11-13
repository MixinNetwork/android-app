package one.mixin.android.ui.wallet.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NetworkOption(
    val chainId: String,
    val name: String,
) : Parcelable
