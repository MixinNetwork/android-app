package one.mixin.android.vo.market

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "market_favored")
data class MarketFavored(
    @PrimaryKey
    @SerializedName("coin_id")
    @ColumnInfo(name = "coin_id")
    val coinId: String,
    @SerializedName("is_favored")
    @ColumnInfo(name = "is_favored")
    val isFavored: Boolean,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    var createdAt: String,
)
