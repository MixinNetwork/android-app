package one.mixin.android.vo.safe

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
class SafeWithdrawal(
    @SerializedName("withdrawal_hash")
    @SerialName("withdrawal_hash")
    val withdrawalHash: String,
    @SerializedName("Receiver")
    @SerialName("Receiver")
    val receiver: String
) : Parcelable
