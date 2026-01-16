package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.db.BaseDao
import java.math.BigDecimal

@Dao
interface WalletOutputDao: BaseDao<WalletOutput> {
    @Query("SELECT * FROM outputs WHERE address = :address AND asset_id = :assetId AND status IN ('unspent', 'pending') ORDER BY created_at ASC")
    suspend fun outputsByAddress(address: String, assetId: String): List<WalletOutput>

    @Query("SELECT * FROM outputs WHERE address = :address AND asset_id = :assetId ORDER BY created_at DESC")
    fun observeOutputsByAddress(address: String, assetId: String): Flow<List<WalletOutput>>

    @Query("SELECT amount FROM outputs WHERE address = :address AND asset_id = :assetId AND status IN ('unspent', 'pending')")
    suspend fun findPendingAndUnspentAmounts(address: String, assetId: String): List<String>

    @Query("SELECT * FROM outputs WHERE address = :address AND asset_id = :assetId AND status IN ('unspent', 'signed') ORDER BY created_at DESC")
    suspend fun outputsByAddressForSigning(address: String, assetId: String): List<WalletOutput>

    @Query("SELECT * FROM outputs WHERE transaction_hash = :hash AND output_index = :outputIndex AND asset_id = :assetId LIMIT 1")
    suspend fun outputByOutpoint(hash: String, outputIndex: Long, assetId: String): WalletOutput?

    @Query("SELECT DISTINCT transaction_hash FROM outputs WHERE address = :address AND asset_id = :assetId AND status = 'pending'")
    suspend fun findPendingTransactionHashes(address: String, assetId: String): List<String>

    @Query("DELETE FROM outputs WHERE address = :address AND asset_id = :assetId AND status = 'pending' AND transaction_hash IN (:hashes)")
    suspend fun deletePendingByTransactionHashes(address: String, assetId: String, hashes: List<String>): Int

    @Query("DELETE FROM outputs WHERE transaction_hash = :hash AND asset_id = :assetId")
    suspend fun deleteByTransactionHash(hash: String, assetId: String): Int

    @Query("DELETE FROM outputs WHERE address = :address AND asset_id = :assetId")
    suspend fun deleteByAddress(address: String, assetId: String)

    @Query("SELECT output_id FROM outputs WHERE output_id IN (:ids) AND status = 'signed'")
    suspend fun findSignedOutputIds(ids: List<String>): List<String>

    @Query("SELECT output_id FROM outputs WHERE address = :address AND asset_id = :assetId AND status = 'unspent' AND output_id NOT IN (:remoteOutputIds)")
    suspend fun findLocalUnspentOutputIdsNotIn(address: String, assetId: String, remoteOutputIds: List<String>): List<String>

    @Query("SELECT output_id FROM outputs WHERE address = :address AND asset_id = :assetId AND status = 'unspent'")
    suspend fun findLocalUnspentOutputIds(address: String, assetId: String): List<String>

    @Query("UPDATE outputs SET status = 'signed' WHERE output_id IN (:outputIds) AND status != 'pending'")
    suspend fun updateOutputsToSigned(outputIds: List<String>): Int

    @Query("DELETE FROM outputs WHERE transaction_hash = :hash AND output_index = :outputIndex AND address = :address AND asset_id = :assetId AND status = 'signed'")
    suspend fun deleteSignedByOutpoint(hash: String, outputIndex: Long, address: String, assetId: String): Int

    @Transaction
    suspend fun mergeOutputsForAddress(address: String, assetId: String, remoteOutputs: List<WalletOutput>) {
        val pendingTransactionHashes: Set<String> = findPendingTransactionHashes(address, assetId).toSet()
        val remoteTransactionHashes: Set<String> = remoteOutputs.map { it.transactionHash }.toSet()
        val pendingTransactionHashesToOverwrite: List<String> = pendingTransactionHashes.intersect(remoteTransactionHashes).toList()
        if (pendingTransactionHashesToOverwrite.isNotEmpty()) {
            deletePendingByTransactionHashes(address, assetId, pendingTransactionHashesToOverwrite)
        }
        val remoteOutputIds: List<String> = remoteOutputs.map { it.outputId }
        val signedOutputIds: List<String> = if (remoteOutputIds.isEmpty()) emptyList() else findSignedOutputIds(remoteOutputIds)
        val outputsToInsert: List<WalletOutput> = if (signedOutputIds.isEmpty()) {
            remoteOutputs
        } else {
            remoteOutputs.filterNot { signedOutputIds.contains(it.outputId) }
        }
        if (outputsToInsert.isNotEmpty()) {
            insertListSuspend(outputsToInsert)
        }
        val localUnspentNotInRemote: List<String> = if (remoteOutputIds.isEmpty()) {
            findLocalUnspentOutputIds(address, assetId)
        } else {
            findLocalUnspentOutputIdsNotIn(address, assetId, remoteOutputIds)
        }
        if (localUnspentNotInRemote.isNotEmpty()) {
            updateOutputsToSigned(localUnspentNotInRemote)
        }
    }

    suspend fun sumPendingAndUnspentAmount(address: String, assetId: String): BigDecimal {
        val amounts: List<String> = findPendingAndUnspentAmounts(address, assetId)
        if (amounts.isEmpty()) return BigDecimal.ZERO
        var total: BigDecimal = BigDecimal.ZERO
        for (amount: String in amounts) {
            val value: BigDecimal = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
            total = total.add(value)
        }
        return total
    }
}
