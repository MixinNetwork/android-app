package one.mixin.android.db

import androidx.room.Dao
import one.mixin.android.vo.market.PriceInfo

@Dao
interface PriceInfoDao : BaseDao<PriceInfo>