package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.ui.home.web3.components.InscriptionState
import one.mixin.android.vo.UtxoItem
import one.mixin.android.vo.safe.Output
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.SafeCollection
import timber.log.Timber

@Dao
interface OutputDao : BaseDao<Output> {
    @Query("SELECT * FROM outputs WHERE (state = 'unspent' OR state = 'pending') AND asset = :asset AND (inscription_hash IS NULL OR inscription_hash = '') ORDER BY CASE WHEN state = 'pending' THEN 1 ELSE 0 END, sequence ASC LIMIT :limit")
    suspend fun findUnspentOutputsByAsset(
        limit: Int,
        asset: String,
    ): List<Output>

    @Query("SELECT * FROM outputs WHERE (state = 'unspent' OR state = 'pending') AND asset = :asset AND (inscription_hash IS NULL OR inscription_hash = '') ORDER BY sequence ASC LIMIT :limit OFFSET :offset")
    suspend fun findUnspentOutputsByAssetOffset(
        limit: Int,
        asset: String,
        offset: Int,
    ): List<Output>

    @Query("SELECT * FROM outputs WHERE (state = 'unspent' OR state = 'pending') AND asset = :asset AND inscription_hash = :inscriptionHash ORDER BY CASE WHEN state = 'pending' THEN 1 ELSE 0 END, sequence ASC LIMIT :limit")
    suspend fun findUnspentInscriptionByAssetHash(
        limit: Int,
        asset: String,
        inscriptionHash: String,
    ): List<Output>

    @Query("SELECT output_id FROM outputs WHERE output_id IN (:ids) AND state = 'signed'")
    fun findSignedOutput(ids: List<String>): List<String>

    @Query("SELECT sequence FROM outputs ORDER BY sequence DESC LIMIT 1")
    suspend fun findLatestOutputSequence(): Long?

    @Query("SELECT sequence FROM outputs WHERE asset = :asset ORDER BY sequence DESC LIMIT 1")
    suspend fun findLatestOutputSequenceByAsset(asset: String): Long?

    @Query("UPDATE outputs SET state = 'signed' WHERE output_id IN (:outputIds)")
    fun updateUtxoToSigned(outputIds: List<String>): Int

    @Query("SELECT output_id FROM outputs WHERE (state = 'unspent' OR state = 'pending') AND output_id IN (:outputIds)")
    fun getUnsignedOutputs(outputIds: List<String>): List<String>

    @Query("SELECT * FROM outputs WHERE asset = :asset ORDER BY created_at DESC, rowid DESC")
    fun utxoItem(asset: String): PagingSource<Int, UtxoItem>

    @Query("DELETE FROM outputs WHERE asset = :asset AND sequence >= :offset")
    suspend fun deleteByKernelAssetIdAndOffset(
        asset: String,
        offset: Long,
    )

    @Query("DELETE FROM outputs WHERE output_id =:outputId")
    suspend fun removeUtxo(outputId: String)

    @Query(
        """
        SELECT i.collection_hash, i.inscription_hash, ic.name, i.sequence, i.content_type, i.content_url, t.icon_url FROM outputs o 
        INNER JOIN inscription_items i ON i.inscription_hash == o.inscription_hash
        INNER JOIN inscription_collections ic on ic.collection_hash = i.collection_hash
        INNER JOIN tokens t on t.collection_hash = i.collection_hash
        WHERE (o.state = 'unspent' OR o.state = 'pending')
        ORDER BY 
            CASE 
                WHEN :orderBy = 'Recent' THEN o.sequence 
            END DESC,
            CASE 
                WHEN :orderBy = 'Alphabetical' THEN ic.name 
            END ASC
        """,
    )
    fun collectibles(orderBy: String): LiveData<List<SafeCollectible>>

    @Query(
        """
        SELECT i.collection_hash, i.inscription_hash, ic.name, i.sequence, i.content_type, i.content_url, t.icon_url FROM outputs o 
        INNER JOIN inscription_items i ON i.inscription_hash == o.inscription_hash
        INNER JOIN inscription_collections ic on ic.collection_hash = i.collection_hash
        INNER JOIN tokens t on t.collection_hash = i.collection_hash
        WHERE (o.state = 'unspent' OR o.state = 'pending') AND ic.collection_hash = :collectionHash
        ORDER BY i.sequence ASC
        """,
    )
    fun collectiblesByHash(collectionHash: String): LiveData<List<SafeCollectible>>

    @Query(
        """
        SELECT ic.collection_hash, ic.name, ic.icon_url, ic.description, count(i.inscription_hash) AS inscription_count 
        FROM outputs o
        INNER JOIN inscription_items i ON i.inscription_hash = o.inscription_hash
        INNER JOIN inscription_collections ic ON ic.collection_hash = i.collection_hash
        WHERE (o.state = 'unspent' OR o.state = 'pending')
        GROUP BY ic.collection_hash 
         ORDER BY 
            CASE 
                WHEN :orderBy = 'Recent' THEN o.sequence 
            END DESC,
            CASE 
                WHEN :orderBy = 'Alphabetical' THEN ic.name 
            END ASC
        """,
    )
    fun collections(orderBy: String): LiveData<List<SafeCollection>>

    @Query(
        """
        SELECT ic.collection_hash, ic.name, ic.icon_url, ic.description, COUNT(CASE WHEN (o.state = 'unspent' OR o.state = 'pending') THEN o.inscription_hash END) AS inscription_count
        FROM inscription_collections ic 
        INNER JOIN inscription_items i ON ic.collection_hash = i.collection_hash
        LEFT JOIN outputs o ON o.inscription_hash = i.inscription_hash
        WHERE  ic.collection_hash = :hash 
        """,
    )
    fun collectionByHash(hash: String): LiveData<SafeCollection?>


    // Get the latest inscription, inscription UTXO cannot be separated
    @Query(
        """
        SELECT ic.name, i.sequence, t.symbol, t.price_usd, t.icon_url, o.state, i.content_url, i.content_type, i.owner, i.traits, ic.unit, ic.treasury 
        FROM inscription_items i
        LEFT JOIN inscription_collections ic on ic.collection_hash = i.collection_hash
        LEFT JOIN outputs o ON i.inscription_hash == o.inscription_hash
        LEFT JOIN tokens t on t.collection_hash = i.collection_hash
        WHERE i.inscription_hash = :hash
        ORDER BY o.sequence DESC LIMIT 1
    """,
    )
    fun inscriptionStateByHash(hash: String): LiveData<InscriptionState?>

    @Query(
        """
        SELECT * FROM outputs WHERE inscription_hash = :inscriptionHash AND (state = 'unspent' OR state = 'pending')
    """,
    )
    suspend fun findUnspentOutputByHash(inscriptionHash: String): Output?

    // Get the latest inscription, inscription UTXO cannot be separated
    @Query(
        """
        SELECT * FROM outputs WHERE inscription_hash = :inscriptionHash ORDER BY sequence DESC LIMIT 1
    """,
    )
    suspend fun findOutputByHash(inscriptionHash: String): Output?

    @Query(
        """
        SELECT DISTINCT inscription_hash FROM outputs WHERE inscription_hash IS NOT NULL AND (state = 'unspent' OR state = 'pending') 
    """,
    )
    suspend fun findUnspentInscriptionHash(): List<String>

    @Transaction
    fun insertUnspentOutputs(outputs: List<Output>) {
        val signed = findSignedOutput(outputs.map { it.outputId })
        if (signed.isEmpty()) {
            insertList(outputs)
        } else {
            Timber.d("Insert filter ${signed.joinToString(", ")}")
            // Exclude signed data
            val unsignedData = outputs.filterNot { signed.contains(it.outputId) }
            insertList(unsignedData)
        }
    }
}
