package one.mixin.android.api.response.web3

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.db.web3.vo.Web3Address

@Parcelize
data class Web3WalletResponse(
    @SerializedName("wallet_id")
    val id: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String,

    @SerializedName("addresses")
    val addresses: List<Web3Address>?
) : Parcelable