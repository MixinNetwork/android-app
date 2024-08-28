package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.market.MarketId

@Dao
interface MarketIdsDao : BaseDao<MarketId>