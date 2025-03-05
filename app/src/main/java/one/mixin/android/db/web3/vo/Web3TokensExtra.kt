package one.mixin.android.db.web3.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tokens_extra")
data class Web3TokensExtra(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    
    @ColumnInfo(name = "hidden")
    val hidden: Boolean? = null,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)