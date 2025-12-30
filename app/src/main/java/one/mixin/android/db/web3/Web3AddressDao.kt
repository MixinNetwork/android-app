package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.vo.ChainItem

@Dao
interface Web3AddressDao : BaseDao<Web3Address> {

    @Query("SELECT * FROM addresses")
    suspend fun getAddress(): List<Web3Address>

    @Query("SELECT * FROM addresses WHERE wallet_id = :walletId AND chain_id = :chainId")
    suspend fun getAddressesByChainId(walletId: String, chainId: String): Web3Address?

    @Query("SELECT * FROM addresses WHERE wallet_id = :walletId")
    suspend fun getAddressesByWalletId(walletId: String): List<Web3Address>

    @Query("SELECT * FROM addresses WHERE wallet_id = :walletId GROUP BY destination")
    suspend fun getAddressesGroupedByDestination(walletId: String): List<Web3Address>

    @Query("DELETE FROM addresses WHERE wallet_id = :walletId")
    suspend fun deleteByWalletId(walletId: String)

    @Query("SELECT COUNT(*) FROM addresses WHERE wallet_id = :walletId")
    suspend fun countAddressesByWalletId(walletId: String): Int

    @Query("SELECT * FROM addresses WHERE wallet_id = :walletId LIMIT 1")
    suspend fun getFirstAddressByWalletId(walletId: String): Web3Address?

    @Query("SELECT COUNT(1) > 0 FROM addresses WHERE destination IN (:destinations)")
    suspend fun anyAddressExists(destinations: List<String>): Boolean

    @Query("SELECT a.chain_id, c.icon_url, c.name, a.destination FROM addresses a LEFT JOIN chains c ON c.chain_id = a.chain_id WHERE a.wallet_id = :walletId")
    suspend fun getChainItemByWalletId(walletId: String): List<ChainItem>

    @Query("SELECT EXISTS(SELECT 1 FROM addresses WHERE wallet_id = :walletId AND destination = :address)")
    suspend fun isAddressMatch(walletId: String, address: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM addresses WHERE destination = :address)")
    suspend fun addressMatch(address: String): Boolean

    @Query(
        """
        SELECT wallet_id AS id, category, name, created_at AS createdAt, updated_at AS updatedAt,
               NULL AS safeRole, NULL AS safeChainId, NULL AS safeAddress, NULL AS safeUrl
        FROM wallets
        WHERE wallet_id IN (SELECT wallet_id FROM addresses WHERE destination = :destination)
        LIMIT 1
        """,
    )
    suspend fun getWalletByDestination(destination: String): WalletItem? // Only find with wallets

    @Query(
        """
        SELECT wallet_id AS id, 'mixin_safe' AS category, name, created_at AS createdAt, updated_at AS updatedAt,
               role AS safeRole, chain_id AS safeChainId, address AS safeAddress, url AS safeUrl
        FROM safe_wallets
        WHERE address = :destination AND chain_id = :chainId
        LIMIT 1
        """,
    )
    suspend fun getSafeWalletByAddress(destination: String, chainId: String): WalletItem? // Only find with safe_wallets
}
