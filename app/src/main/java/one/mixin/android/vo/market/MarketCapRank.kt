package one.mixin.android.vo.market

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(
    tableName = "market_cap_ranks",
)
class MarketCapRank(
    @PrimaryKey
    @ColumnInfo("coin_id")
    val coinId: String,
    @ColumnInfo("market_cap_rank")
    val marketCapRank: String,
    @ColumnInfo("updated_at")
    val updatedAt: String,
)