package one.mixin.android.db.web3

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import one.mixin.android.Constants
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3Wallet
import timber.log.Timber

@Dao
interface Web3WalletDao : BaseDao<Web3Wallet> {

    @Query("SELECT * FROM wallets WHERE wallet_id != :excludeWalletId AND name LIKE '%' || :query || '%'")
    suspend fun getWalletsExcludingByName(excludeWalletId: String, query: String): List<Web3Wallet>

    @Query("SELECT * FROM wallets")
    fun getWallets(): Flow<List<Web3Wallet>>

    @Query("SELECT wallet_id FROM wallets WHERE category = 'classic' LIMIT 1")
    suspend fun getClassicWalletId(): String?

    @Query("SELECT * FROM wallets WHERE wallet_id = :walletId")
    suspend fun getWalletById(walletId: String): Web3Wallet?

    @Query("SELECT * FROM wallets ORDER BY created_at ASC")
    suspend fun getAllWallets(): List<Web3Wallet>

    @Query("DELETE FROM wallets WHERE wallet_id = :walletId")
    suspend fun deleteWalletById(walletId: String)

    @Query("DELETE FROM wallets WHERE wallet_id = :walletId")
    suspend fun deleteWallet(walletId: String)

    @Query("UPDATE wallets SET name = :newName WHERE wallet_id = :walletId")
    suspend fun updateWalletName(walletId: String, newName: String)

    @Query("SELECT COUNT(*) FROM wallets WHERE category = 'private'")
    suspend fun countPrivateWallets(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun getWalletsCount(): Int

    @Query("DELETE FROM wallets")
    suspend fun deleteAllWallets()
}

private fun getEncryptedSharedPreferences(context: Context): SharedPreferences? {
    return try {
        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            Constants.Tip.ENCRYPTED_WEB3_KEY,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}

fun List<Web3Wallet>.updateWithLocalKeyInfo(context: Context): List<Web3Wallet> {
    val storage = getEncryptedSharedPreferences(context)
    return if (storage == null) {
        this
    } else {
        this.onEach {
            it.hasLocalPrivateKey = it.category == "private" || storage.contains(it.id)
        }
    }
}

fun Web3Wallet.updateWithLocalKeyInfo(context: Context): Web3Wallet {
    val storage = getEncryptedSharedPreferences(context)
    if (storage != null) {
        this.hasLocalPrivateKey = category == "private" || storage.contains(this.id)
    }
    return this
}
