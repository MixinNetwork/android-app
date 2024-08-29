package one.mixin.android.db

import androidx.room.Dao
import one.mixin.android.vo.market.MarketCoin

@Dao
interface MarketCoinDao : BaseDao<MarketCoin>