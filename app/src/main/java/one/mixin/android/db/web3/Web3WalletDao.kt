package one.mixin.android.db.web3

import android.content.Context
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.vo.WalletCategory

@Dao
interface Web3WalletDao : BaseDao<Web3Wallet> {

    @Query("SELECT DISTINCT w.* FROM wallets w INNER JOIN addresses a ON w.wallet_id = a.wallet_id WHERE w.wallet_id != :excludeWalletId AND a.chain_id = :chainId AND w.name LIKE '%' || :query || '%'")
    suspend fun getWalletsExcludingByName(excludeWalletId: String, chainId: String, query: String): List<Web3Wallet>

    @Query("SELECT * FROM wallets ORDER BY created_at ASC")
    fun getWallets(): Flow<List<Web3Wallet>>

    @Query("SELECT wallet_id FROM wallets WHERE category = 'classic' LIMIT 1")
    suspend fun getClassicWalletId(): String?

    @Query("SELECT * FROM wallets WHERE wallet_id = :walletId")
    suspend fun getWalletById(walletId: String): Web3Wallet?

    @Query("SELECT * FROM wallets ORDER BY created_at ASC")
    suspend fun getAllWallets(): List<Web3Wallet>

    @Query("SELECT name FROM wallets WHERE category IN (:categories)")
    suspend fun getAllWalletNames(categories: List<String>): List<String?>

    @Query("SELECT * FROM wallets WHERE category = 'classic' ORDER BY created_at ASC")
    suspend fun getAllClassicWallets(): List<Web3Wallet>

    @Query("DELETE FROM wallets WHERE wallet_id = :walletId")
    suspend fun deleteWalletById(walletId: String)

    @Query("DELETE FROM wallets WHERE wallet_id = :walletId")
    suspend fun deleteWallet(walletId: String)

    @Query("UPDATE wallets SET name = :newName WHERE wallet_id = :walletId")
    suspend fun updateWalletName(walletId: String, newName: String)

    @Query("SELECT COUNT(*) FROM wallets WHERE category = 'imported_mnemonic'")
    suspend fun countPrivateWallets(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun getWalletsCount(): Int

    @Query("DELETE FROM wallets")
    suspend fun deleteAllWallets()
}

fun List<Web3Wallet>.updateWithLocalKeyInfo(context: Context): List<Web3Wallet> {
    return this.onEach {
        it.updateWithLocalKeyInfo(context)
    }
}

fun Web3Wallet.updateWithLocalKeyInfo(context: Context): Web3Wallet {
    if (this.category == WalletCategory.WATCH_ADDRESS.value) {
        this.hasLocalPrivateKey = false
        return this
    }else if (this.category == WalletCategory.CLASSIC.value) {
        this.hasLocalPrivateKey = true
        return this
    }else {
        this.hasLocalPrivateKey = CryptoWalletHelper.hasPrivateKey(context, this.id)
        return this
    }
}
