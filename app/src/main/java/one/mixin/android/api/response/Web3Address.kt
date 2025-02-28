package one.mixin.android.api.response

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "web3_addresses",
)
@Parcelize
data class Web3Address(
    @PrimaryKey
    @ColumnInfo(name = "address_id")
    @SerializedName("address_id")
    val addressId: String,
    
    @ColumnInfo(name = "destination")
    @SerializedName("destination")
    val destination: String,
    
    @ColumnInfo(name = "tag")
    @SerializedName("tag")
    val tag: String?,

    @ColumnInfo(name = "user_id")
    @SerializedName("user_id")
    val userId: String,
    
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,
    
) : Parcelable
