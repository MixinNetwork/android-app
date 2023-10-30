package one.mixin.android.db

import androidx.room.Dao
import one.mixin.android.vo.safe.DepositEntry

@Dao
interface DepositDao : BaseDao<DepositEntry>
