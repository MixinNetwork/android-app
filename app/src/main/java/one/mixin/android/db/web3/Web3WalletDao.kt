package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.db.BaseDao

@Dao
interface Web3WalletDao : BaseDao<Web3Wallet> {
    
    @Query("SELECT * FROM wallets")
    fun getWallets(): Flow<List<Web3Wallet>>

    @Query("SELECT wallet_id FROM wallets WHERE category = 'classic'")
    suspend fun getClassicWalletId(): String?

    @Query("SELECT * FROM wallets WHERE wallet_id = :walletId")
    suspend fun getWalletById(walletId: String): Web3Wallet?
    
    @Query("SELECT * FROM wallets ORDER BY created_at DESC")
    suspend fun getAllWallets(): List<Web3Wallet>
    
    @Query("DELETE FROM wallets WHERE wallet_id = :walletId")
    suspend fun deleteWalletById(walletId: String)
    
    @Transaction
    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun getWalletsCount(): Int
}
