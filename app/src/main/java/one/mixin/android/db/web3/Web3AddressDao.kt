package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3Address

@Dao
interface Web3AddressDao : BaseDao<Web3Address> {

    @Query("SELECT * FROM addresses")
    suspend fun getAddress(): List<Web3Address>

    @Query("SELECT * FROM addresses WHERE wallet_id = :walletId AND chain_id = :chainId")
    suspend fun getAddressesByChainId(walletId: String, chainId: String): Web3Address?

    @Query("SELECT * FROM addresses WHERE wallet_id = :walletId")
    suspend fun getAddressesByWalletId(walletId: String): List<Web3Address>

    @Query("DELETE FROM addresses WHERE wallet_id = :walletId")
    suspend fun deleteByWalletId(walletId: String)

    @Query("SELECT COUNT(*) FROM addresses WHERE wallet_id = :walletId")
    suspend fun countAddressesByWalletId(walletId: String): Int

    @Query("SELECT * FROM addresses WHERE wallet_id = :walletId LIMIT 1")
    suspend fun getFirstAddressByWalletId(walletId: String): Web3Address?

    @Query("SELECT COUNT(1) > 0 FROM addresses WHERE destination IN (:destinations)")
    suspend fun anyAddressExists(destinations: List<String>): Boolean

}
