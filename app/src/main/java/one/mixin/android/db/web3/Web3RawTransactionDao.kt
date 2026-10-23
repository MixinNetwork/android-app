package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room3.Dao
import androidx.room3.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3RawTransaction

@Dao
interface Web3RawTransactionDao : BaseDao<Web3RawTransaction> {
    
    @Query("SELECT * FROM raw_transactions WHERE state = 'pending' AND account IN (SELECT DISTINCT destination FROM addresses WHERE wallet_id = :walletId)")
    suspend fun getPendingRawTransactions(walletId:String): List<Web3RawTransaction>

    @Query("SELECT COUNT(*) FROM raw_transactions WHERE state = 'pending' AND account IN (SELECT DISTINCT destination FROM addresses WHERE wallet_id = :walletId)")
    fun getPendingRawTransactionCount(walletId: String): LiveData<Int>

    @Query("SELECT * FROM raw_transactions WHERE state = 'pending' AND chain_id = :chainId AND account IN (SELECT DISTINCT destination FROM addresses WHERE wallet_id = :walletId)")
    suspend fun getPendingRawTransactions(walletId:String, chainId: String): List<Web3RawTransaction>

    @Query("SELECT * FROM raw_transactions WHERE state = 'pending' AND account = :account AND chain_id = :chainId")
    suspend fun getPendingRawTransactionsByAccount(account: String, chainId: String): List<Web3RawTransaction>

    @Query("SELECT * FROM raw_transactions WHERE hash = :hash AND chain_id = :chainId AND account IN (SELECT DISTINCT destination FROM addresses WHERE wallet_id = :walletId)")
    suspend fun getRawTransactionByHashAndChain(walletId:String, hash: String, chainId: String): Web3RawTransaction?

    @Query("DELETE FROM raw_transactions WHERE hash = :hash AND chain_id = :chainId")
    suspend fun deleteByHashAndChain(hash: String, chainId: String)

    @Query("SELECT nonce FROM raw_transactions WHERE chain_id = :chainId AND state = 'pending' AND raw NOT LIKE 'gasless:%' AND account IN (SELECT DISTINCT destination FROM addresses WHERE wallet_id = :walletId) ORDER BY nonce DESC LIMIT 1")
    suspend fun getNonce(walletId:String, chainId: String): String?
}
