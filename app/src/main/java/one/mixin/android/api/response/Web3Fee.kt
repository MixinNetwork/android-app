package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Web3Fee(
    @SerializedName("fungible_id")
    val fungibleId: String,
    val name: String,
    val symbol: String,
    @SerializedName("icon_url")
    val iconUrl: String,
    val amount: String,
    val price: String,
) : Parcelable
