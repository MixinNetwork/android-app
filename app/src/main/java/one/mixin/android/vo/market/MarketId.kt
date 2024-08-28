package one.mixin.android.vo.market

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Entity(tableName = "market_ids", primaryKeys = ["coin_id", "asset_id"])
data class MarketId(
    @SerializedName("coin_id")
    @ColumnInfo(name = "coin_id")
    val coinId: String,
    @SerializedName("asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    var createdAt: String,
)
