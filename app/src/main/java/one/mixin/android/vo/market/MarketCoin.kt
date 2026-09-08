package one.mixin.android.vo.market

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
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
