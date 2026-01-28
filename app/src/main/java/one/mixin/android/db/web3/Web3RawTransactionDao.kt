package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3RawTransaction

@Dao
interface Web3RawTransactionDao : BaseDao<Web3RawTransaction> {
    
    @Query("SELECT * FROM raw_transactions WHERE state = 'pending' AND account IN (SELECT DISTINCT destination FROM addresses WHERE wallet_id = :walletId)")
    suspend fun getPendingRawTransactions(walletId:String): List<Web3RawTransaction>

    @Query("SELECT * FROM raw_transactions WHERE state = 'pending' AND chain_id = :chainId AND account IN (SELECT DISTINCT destination FROM addresses WHERE wallet_id = :walletId)")
    suspend fun getPendingRawTransactions(walletId:String, chainId: String): List<Web3RawTransaction>

    @Query("SELECT * FROM raw_transactions WHERE state = 'pending' AND account = :account")
    suspend fun getPendingRawTransactionsByAccount(account: String): List<Web3RawTransaction>

    @Query("SELECT * FROM raw_transactions WHERE hash = :hash AND chain_id = :chainId AND account IN (SELECT DISTINCT destination FROM addresses WHERE wallet_id = :walletId)")
    suspend fun getRawTransactionByHashAndChain(walletId:String, hash: String, chainId: String): Web3RawTransaction?

    @Query("SELECT nonce FROM raw_transactions WHERE chain_id = :chainId AND state = 'pending' AND account IN (SELECT DISTINCT destination FROM addresses WHERE wallet_id = :walletId) ORDER BY nonce DESC LIMIT 1")
    suspend fun getNonce(walletId:String, chainId: String): String?
}
