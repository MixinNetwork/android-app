package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants

@Entity(
    tableName = "addresses",
)
@Parcelize
data class Web3Address(
    @PrimaryKey
    @ColumnInfo(name = "address_id")
    @SerializedName("address_id")
    val addressId: String,

    @ColumnInfo(name = "wallet_id")
    @SerializedName("wallet_id")
    val walletId: String,

    @ColumnInfo(name = "chain_id")
    @SerializedName("chain_id")
    val chainId: String,

    @ColumnInfo(name = "destination")
    @SerializedName("destination")
    val destination: String,

    @ColumnInfo(name = "path")
    @SerializedName("path")
    val path: String?,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,

) : Parcelable {
    fun isEvmAddress(): Boolean {
        return chainId != Constants.ChainId.SOLANA_CHAIN_ID
    }
}