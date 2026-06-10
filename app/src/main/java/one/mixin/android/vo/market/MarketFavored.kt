package one.mixin.android.vo.market

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
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
