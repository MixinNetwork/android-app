package one.mixin.android.db.web3.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "tokens_extra", primaryKeys = ["wallet_id", "asset_id"]
)
data class Web3TokensExtra(

    @ColumnInfo(name = "wallet_id")
    val walletId: String,

    @ColumnInfo(name = "asset_id")
    val assetId: String,

    @ColumnInfo(name = "hidden")
    val hidden: Boolean? = null,
)