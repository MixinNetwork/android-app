package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.api.response.Web3Transaction
import one.mixin.android.db.BaseDao

@Dao
interface Web3TransactionDao : BaseDao<Web3Transaction> {

    @Query("SELECT * FROM web3_transactions")
    fun web3Transactions(): LiveData<List<Web3Transaction>>
}
