package one.mixin.android.db.web3

import android.content.Context
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.vo.WalletCategory

@Dao
interface Web3WalletDao : BaseDao<Web3Wallet> {

    @Query("""
        SELECT wallet_id AS id, category, name, created_at AS createdAt, updated_at AS updatedAt,
               NULL AS safeRole, NULL AS safeChainId, NULL AS safeAddress, NULL AS safeUrl
        FROM wallets
        WHERE wallet_id != :excludeWalletId
          AND name LIKE '%' || :query || '%'
          AND EXISTS (SELECT 1 FROM addresses a WHERE a.wallet_id = wallets.wallet_id AND a.chain_id = :chainId)
        UNION ALL
        SELECT wallet_id AS id, 'mixin_safe' AS category, name, created_at AS createdAt, updated_at AS updatedAt,
               role AS safeRole, chain_id AS safeChainId, address AS safeAddress, url AS safeUrl
        FROM safe_wallets
        WHERE wallet_id != :excludeWalletId
          AND name LIKE '%' || :query || '%'
          AND chain_id = :chainId
        ORDER BY createdAt ASC
    """)
    suspend fun getWalletsExcludingByName(excludeWalletId: String, chainId: String, query: String): List<WalletItem>

    @Query("""
        SELECT wallet_id AS id, category, name, created_at AS createdAt, updated_at AS updatedAt,
               NULL AS safeRole, NULL AS safeChainId, NULL AS safeAddress, NULL AS safeUrl
        FROM wallets
        WHERE wallet_id != :excludeWalletId AND name LIKE '%' || :query || '%'
        UNION ALL
        SELECT wallet_id AS id, 'mixin_safe' AS category, name, created_at AS createdAt, updated_at AS updatedAt,
               role AS safeRole, chain_id AS safeChainId, address AS safeAddress, url AS safeUrl
        FROM safe_wallets
        WHERE wallet_id != :excludeWalletId AND name LIKE '%' || :query || '%'
        ORDER BY createdAt ASC
    """)
    suspend fun getWalletsExcludingByNameAllChains(excludeWalletId: String, query: String): List<WalletItem>

    @Query("""
        SELECT wallet_id AS id, category, name, created_at AS createdAt, updated_at AS updatedAt,
               NULL AS safeRole, NULL AS safeChainId, NULL AS safeAddress, NULL AS safeUrl
        FROM wallets
        UNION ALL
        SELECT wallet_id AS id, 'mixin_safe' AS category, name, created_at AS createdAt, updated_at AS updatedAt,
               role AS safeRole, chain_id AS safeChainId, address AS safeAddress, url AS safeUrl
        FROM safe_wallets
        ORDER BY createdAt ASC
    """)
    fun getWallets(): Flow<List<WalletItem>>

    @Query("""
        SELECT wallet_id AS id, category, name, created_at AS createdAt, updated_at AS updatedAt,
               NULL AS safeRole, NULL AS safeChainId, NULL AS safeAddress, NULL AS safeUrl
        FROM wallets
        UNION ALL
        SELECT wallet_id AS id, 'mixin_safe' AS category, name, created_at AS createdAt, updated_at AS updatedAt,
               role AS safeRole, chain_id AS safeChainId, address AS safeAddress, url AS safeUrl
        FROM safe_wallets
        ORDER BY createdAt ASC
    """)
    suspend fun getAllWallets(): List<WalletItem>

    @Query("SELECT wallet_id FROM wallets WHERE category = 'classic' ORDER BY created_at ASC LIMIT 1 ")
    suspend fun getClassicWalletId(): String?

    @Query("""
        SELECT wallet_id AS id, category, name, created_at AS createdAt, updated_at AS updatedAt,
               NULL AS safeRole, NULL AS safeChainId, NULL AS safeAddress, NULL AS safeUrl
        FROM wallets
        WHERE wallet_id = :walletId
        UNION ALL
        SELECT wallet_id AS id, 'mixin_safe' AS category, name, created_at AS createdAt, updated_at AS updatedAt,
               role AS safeRole, chain_id AS safeChainId, address AS safeAddress, url AS safeUrl
        FROM safe_wallets
        WHERE wallet_id = :walletId
        LIMIT 1
    """)
    suspend fun getWalletById(walletId: String): WalletItem?

    @Query("""
        SELECT wallet_id AS id, 'mixin_safe' AS category, name, created_at AS createdAt, updated_at AS updatedAt,
               role AS safeRole, chain_id AS safeChainId, address AS safeAddress, url AS safeUrl
        FROM safe_wallets
        WHERE chain_id = :chainId
        ORDER BY createdAt ASC
    """)
    suspend fun getSafeWalletsByChainId(chainId: String): List<WalletItem>

    @Query("SELECT name FROM wallets WHERE category IN (:categories)")
    suspend fun getAllWalletNames(categories: List<String>): List<String>

    @Query("""
        SELECT wallet_id AS id, category, name, created_at AS createdAt, updated_at AS updatedAt,
               NULL AS safeRole, NULL AS safeChainId, NULL AS safeAddress, NULL AS safeUrl
        FROM wallets
        WHERE category = 'classic'
        ORDER BY createdAt ASC
    """)
    suspend fun getAllClassicWallets(): List<WalletItem>

    @Query("DELETE FROM wallets WHERE wallet_id = :walletId")
    suspend fun deleteWallet(walletId: String)

    @Query("UPDATE wallets SET name = :newName WHERE wallet_id = :walletId")
    suspend fun updateWalletName(walletId: String, newName: String)

    @Query("SELECT COUNT(1) FROM wallets WHERE category = 'imported_mnemonic'")
    suspend fun countPrivateWallets(): Int

    @Transaction
    @Query("SELECT COUNT(1) FROM wallets")
    suspend fun getWalletsCount(): Int

    @Query("DELETE FROM wallets")
    suspend fun deleteAllWallets()
}

fun List<WalletItem>.updateWithLocalKeyInfo(context: Context): List<WalletItem> {
    return this.onEach {
        it.updateWithLocalKeyInfo(context)
    }
}

fun WalletItem.updateWithLocalKeyInfo(context: Context): WalletItem {
    when (this.category) {
        WalletCategory.WATCH_ADDRESS.value -> {
            this.hasLocalPrivateKey = false
            return this
        }
        WalletCategory.CLASSIC.value -> {
            this.hasLocalPrivateKey = true
            return this
        }

        WalletCategory.MIXIN_SAFE.value ->{
            this.hasLocalPrivateKey = false
            return this
        }
        else -> {
            this.hasLocalPrivateKey = CryptoWalletHelper.hasPrivateKey(context, this.id)
            return this
        }
    }
}
