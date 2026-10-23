package one.mixin.android.vo.market

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index

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
