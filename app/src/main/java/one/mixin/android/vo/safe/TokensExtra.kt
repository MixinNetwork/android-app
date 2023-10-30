package one.mixin.android.vo.safe

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = "tokens_extra", indices = [Index(value = arrayOf("kernel_asset_id"))])
data class TokensExtra(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,
    @ColumnInfo(name = "kernel_asset_id")
    @SerializedName("kernel_asset_id")
    val asset: String,
    @SerializedName("hidden")
    @ColumnInfo(name = "hidden")
    var hidden: Boolean?,
    @SerializedName("balance")
    @ColumnInfo(name = "balance")
    val balance: String?,
    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
) : Parcelable
