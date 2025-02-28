package one.mixin.android.db.web3.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 存储代币的额外信息，如是否隐藏
 */
@Entity(tableName = "tokens_extra")
data class Web3TokensExtra(
    @PrimaryKey
    @ColumnInfo(name = "coin_id")
    val coinId: String,
    
    @ColumnInfo(name = "hidden")
    val hidden: Boolean? = null,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)