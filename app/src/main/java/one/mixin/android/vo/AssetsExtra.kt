package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = "assets_extra")
@JsonClass(generateAdapter = true)
data class AssetsExtra(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    @Json(name ="asset_id")
    val assetId: String,
    @Json(name ="hidden")
    @ColumnInfo(name = "hidden")
    var hidden: Boolean?
) : Parcelable
