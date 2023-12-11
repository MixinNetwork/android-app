package one.mixin.android.vo

import androidx.room.ColumnInfo

data class UtxoItem(
    @ColumnInfo("output_id")
    val outputId: String,
    @ColumnInfo("amount")
    val amount: String,
    @ColumnInfo("state")
    val state: String,
    @ColumnInfo("transaction_hash")
    val transactionHash: String,
)
