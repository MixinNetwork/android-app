package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.SafeWallets

@Dao
interface SafeWalletsDao : BaseDao<SafeWallets> {

    @Query("SELECT * FROM safe_wallets WHERE wallet_id = :walletId")
    suspend fun getSafeWalletById(walletId: String): SafeWallets?

    @Query("DELETE FROM safe_wallets WHERE wallet_id = :walletId")
    suspend fun deleteSafeWallet(walletId: String)

    @Query("DELETE FROM safe_wallets WHERE wallet_id NOT IN (:walletIds)")
    suspend fun deleteSafeWalletNotIn(walletIds: List<String>)


    @Query("SELECT * FROM safe_wallets")
    suspend fun getAllSafeWallets(): List<SafeWallets>
}
