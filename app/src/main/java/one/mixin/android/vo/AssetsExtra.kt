package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Deprecated("Deprecated")
@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = "assets_extra")
data class AssetsExtra(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("hidden")
    @ColumnInfo(name = "hidden")
    var hidden: Boolean?,
) : Parcelable
