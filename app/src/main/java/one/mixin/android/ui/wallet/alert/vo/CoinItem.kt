package one.mixin.android.ui.wallet.alert.vo

import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinItem(
    @ColumnInfo("coin_id") val coinId: String, @ColumnInfo("icon_url") val iconUrl: String, @ColumnInfo("symbol") val symbol: String, @ColumnInfo("name") val name: String, @ColumnInfo("current_price") val currentPrice: String
) : Parcelable {
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