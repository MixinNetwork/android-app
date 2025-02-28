package one.mixin.android.api.response

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Entity(tableName = "web3_transactions")
@Parcelize
data class Web3Transaction(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: String,
    
    @ColumnInfo(name = "wallet_id")
    @SerializedName("wallet_id")
    val walletId: String,
    
    @ColumnInfo(name = "hash")
    @SerializedName("hash")
    val hash: String,
    
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,
    
    @ColumnInfo(name = "amount")
    @SerializedName("amount")
    val amount: String,
    
    @ColumnInfo(name = "fee")
    @SerializedName("fee")
    val fee: String,
    
    @ColumnInfo(name = "status")
    @SerializedName("status")
    val status: String,
    
    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,
    
    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String
) : Parcelable
