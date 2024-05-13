package one.mixin.android.db.web3

import androidx.room.Dao
import one.mixin.android.db.BaseDao
import one.mixin.android.vo.web3.Transaction

@Dao
interface TransactionDao : BaseDao<Transaction>