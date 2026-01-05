package one.mixin.android.api.response.web3

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "outputs")
data class WalletOutput(
    @PrimaryKey
    @SerializedName("output_id")
    @ColumnInfo(name = "output_id")
    val outputId: String,

    @SerializedName("transaction_hash")
    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String,

    @SerializedName("output_index")
    @ColumnInfo(name = "output_index")
    val outputIndex: Long,

    @SerializedName("amount")
    @ColumnInfo(name = "amount")
    val amount: String,

    @SerializedName("address")
    @ColumnInfo(name = "address")
    val address: String,

    @SerializedName("pubkey_hex")
    @ColumnInfo(name = "pubkey_hex")
    val pubkeyHex: String? = null,

    @SerializedName("pubkey_type")
    @ColumnInfo(name = "pubkey_type")
    val pubkeyType: String? = null,

    @SerializedName("status")
    @ColumnInfo(name = "status")
    val status: String,

    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)
