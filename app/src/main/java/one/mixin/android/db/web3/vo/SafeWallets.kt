package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Entity(tableName = "safe_wallets")
@Parcelize
class SafeWallets(
    @PrimaryKey
    @ColumnInfo(name = "wallet_id")
    @SerializedName("wallet_id")
    val id: String,

    @ColumnInfo(name = "name")
    @SerializedName("name")
    val name: String,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String,

    @ColumnInfo(name = "role")
    @SerializedName("role")
    val safeRole: String,

    @ColumnInfo(name = "chain_id")
    @SerializedName("chain_id")
    val safeChainId: String,

    @ColumnInfo("address")
    @SerializedName("address")
    val safeAddress: String,

    @ColumnInfo("url")
    @SerializedName("url")
    val safeUrl: String,
): Parcelable