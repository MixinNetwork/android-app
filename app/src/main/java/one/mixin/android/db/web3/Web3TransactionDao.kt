package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import one.mixin.android.api.response.Web3Transaction
import one.mixin.android.db.BaseDao
import one.mixin.android.vo.InscriptionCollection
import one.mixin.android.vo.InscriptionItem
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.Token

@Dao
interface Web3TransactionDao : BaseDao<Web3Transaction> {

    @Query("SELECT * FROM web3_transactions")
    fun web3Transactions(): LiveData<List<Web3Transaction>>

    @RawQuery(observedEntities = [Web3Transaction::class])
    fun allTransactions(query: SupportSQLiteQuery): DataSource.Factory<Int, Web3Transaction>
}
