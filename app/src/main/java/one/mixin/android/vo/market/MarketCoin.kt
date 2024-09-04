package one.mixin.android.vo.market

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "market_coins")
data class MarketCoin(
    @PrimaryKey
    @SerializedName("asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @SerializedName("coin_id")
    @ColumnInfo(name = "coin_id")
    val coinId: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    var createdAt: String,
)
