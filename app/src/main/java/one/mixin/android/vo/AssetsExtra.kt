package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = "assets_extra", foreignKeys = [(ForeignKey(entity = Asset::class,
    onDelete = ForeignKey.NO_ACTION,
    parentColumns = arrayOf("asset_id"),
    childColumns = arrayOf("asset_id")))])
data class AssetsExtra(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("hidden")
    @ColumnInfo(name = "hidden")
    var hidden: Boolean?
) : Parcelable
