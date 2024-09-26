package one.mixin.android.ui.wallet.alert.vo

import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo

data class CoinItem(
    @ColumnInfo("coin_id") val coinId: String, @ColumnInfo("icon_url") val iconUrl: String, @ColumnInfo("symbol") val symbol: String, @ColumnInfo("name") val name: String, @ColumnInfo("current_price") val currentPrice: String
) {
    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<CoinItem>() {
                override fun areItemsTheSame(
                    oldItem: CoinItem,
                    newItem: CoinItem,
                ) =
                    oldItem.coinId == newItem.coinId

                override fun areContentsTheSame(
                    oldItem: CoinItem,
                    newItem: CoinItem,
                ) =
                    oldItem == newItem
            }
    }
}