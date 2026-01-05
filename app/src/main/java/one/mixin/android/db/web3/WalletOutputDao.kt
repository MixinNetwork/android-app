package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.db.BaseDao

@Dao
interface WalletOutputDao: BaseDao<WalletOutput> {
    @Query("SELECT * FROM outputs WHERE address = :address AND status='unspent' ORDER BY created_at DESC")
    suspend fun outputsByAddress(address: String): List<WalletOutput>

    @Query("SELECT * FROM outputs WHERE transaction_hash = :hash AND status='unspent'")
    suspend fun outputsByHash(hash: String): WalletOutput?

    @Query("DELETE FROM outputs WHERE address = :address")
    suspend fun deleteByAddress(address: String)

    @Query("SELECT output_id FROM outputs WHERE output_id IN (:ids) AND status = 'signed'")
    suspend fun findSignedOutputIds(ids: List<String>): List<String>

    @Query("SELECT output_id FROM outputs WHERE address = :address AND status = 'unspent' AND output_id NOT IN (:remoteOutputIds)")
    suspend fun findLocalUnspentOutputIdsNotIn(address: String, remoteOutputIds: List<String>): List<String>

    @Query("SELECT output_id FROM outputs WHERE address = :address AND status = 'unspent'")
    suspend fun findLocalUnspentOutputIds(address: String): List<String>

    @Query("UPDATE outputs SET status = 'signed' WHERE output_id IN (:outputIds)")
    suspend fun updateOutputsToSigned(outputIds: List<String>): Int

    @Transaction
    suspend fun mergeOutputsForAddress(address: String, remoteOutputs: List<WalletOutput>) {
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
            findLocalUnspentOutputIds(address)
        } else {
            findLocalUnspentOutputIdsNotIn(address, remoteOutputIds)
        }
        if (localUnspentNotInRemote.isNotEmpty()) {
            updateOutputsToSigned(localUnspentNotInRemote)
        }
    }
}
