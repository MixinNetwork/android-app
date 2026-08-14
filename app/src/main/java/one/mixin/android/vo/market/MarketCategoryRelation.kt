package one.mixin.android.vo.market

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "market_categories",
    primaryKeys = ["coin_id", "category"],
    indices = [Index(value = ["category"])],
)
data class MarketCategoryRelation(
    @ColumnInfo(name = "coin_id")
    val coinId: String,
    @ColumnInfo(name = "category")
    val category: Int,
)
