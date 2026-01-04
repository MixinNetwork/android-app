package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.db.BaseDao

@Dao
interface WalletOutputDao: BaseDao<WalletOutput> {
    @Query("SELECT * FROM outputs WHERE wallet_id = :walletId AND status='unspent' ORDER BY created_at DESC")
    suspend fun outputsByWalletId(walletId: String): List<WalletOutput>

    @Query("SELECT * FROM outputs WHERE transaction_hash = :hash AND status='unspent'")
    suspend fun outputsByHash(hash: String): WalletOutput?

    @Query("DELETE FROM outputs WHERE wallet_id = :walletId")
    suspend fun deleteByWalletId(walletId: String)
}
