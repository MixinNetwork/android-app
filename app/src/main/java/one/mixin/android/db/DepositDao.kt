package one.mixin.android.db

import androidx.room.Dao
import one.mixin.android.vo.Deposit

@Dao
interface DepositDao : BaseDao<Deposit>
