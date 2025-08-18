package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class WalletAddressResponse(
    @SerializedName("data")
    val data: List<WalletAddress>
) : Parcelable

@Parcelize
data class WalletAddress(
    @SerializedName("address_id")
    val addressId: String,
    
    @SerializedName("destination")
    val destination: String,
    
    @SerializedName("tag")
    val tag: String?,
    
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("position")
    val position: Int,
    
    @SerializedName("created_at")
    val createdAt: String
) : Parcelable
