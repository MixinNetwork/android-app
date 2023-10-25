package one.mixin.android.db

import androidx.room.Dao
import one.mixin.android.vo.safe.Deposit

@Dao
interface DepositDao : BaseDao<Deposit>
