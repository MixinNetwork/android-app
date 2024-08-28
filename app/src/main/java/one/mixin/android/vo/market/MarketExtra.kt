package one.mixin.android.vo.market

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import one.mixin.android.vo.ListConverter

@Entity(tableName = "markets_extra", primaryKeys = ["coin_id", "asset_id"])
@TypeConverters(ListConverter::class)
data class MarketExtra(
    @SerializedName("coin_id")
    @ColumnInfo(name = "coin_id")
    val coinId: String,
    @SerializedName("asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @SerializedName("is_favored")
    @ColumnInfo(name = "is_favored")
    var isFavored: Boolean?,
)
