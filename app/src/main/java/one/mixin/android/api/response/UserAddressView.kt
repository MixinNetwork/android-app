package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserAddressView(
    @SerializedName("destination")
    val destination: String,
    @SerializedName("chain_id")
    val chainId: String
) : Parcelable
