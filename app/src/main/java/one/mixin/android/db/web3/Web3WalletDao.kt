package one.mixin.android.db.web3

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.api.response.Web3Wallet
import one.mixin.android.db.BaseDao

@Dao
interface Web3WalletDao : BaseDao<Web3Wallet> {
    
    @Query("SELECT * FROM web3_wallets")
    fun getWallets(): LiveData<List<Web3Wallet>>
    
    @Query("SELECT * FROM web3_wallets WHERE wallet_id = :walletId")
    suspend fun getWalletById(walletId: String): Web3Wallet?
    
    @Query("SELECT * FROM web3_wallets ORDER BY created_at DESC")
    suspend fun getAllWallets(): List<Web3Wallet>
    
    @Query("DELETE FROM web3_wallets WHERE wallet_id = :walletId")
    suspend fun deleteWalletById(walletId: String)
    
    @Transaction
    @Query("SELECT COUNT(*) FROM web3_wallets")
    suspend fun getWalletsCount(): Int
}
