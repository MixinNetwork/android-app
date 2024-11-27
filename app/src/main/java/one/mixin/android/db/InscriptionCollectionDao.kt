package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.vo.InscriptionCollection
import one.mixin.android.vo.InscriptionItem

@Dao
interface InscriptionCollectionDao : BaseDao<InscriptionCollection> {
    @Query("SELECT collection_hash FROM inscription_collections WHERE collection_hash = :collectionHash")
    suspend fun exits(collectionHash: String): String?

    @Query("SELECT * FROM inscription_collections WHERE collection_hash = :collectionHash")
    suspend fun findInscriptionCollectionByHash(collectionHash: String): InscriptionCollection?

    @Query("SELECT collection_hash FROM inscription_collections")
    suspend fun allCollectionHash(): List<String>

    @Query(
        """
        SELECT i.* FROM inscription_collections ic
        LEFT JOIN inscription_items i on ic.collection_hash = i.collection_hash
        LEFT JOIN outputs o ON i.inscription_hash == o.inscription_hash
        LEFT JOIN tokens t on t.collection_hash = i.collection_hash
        WHERE o.state = 'unspent' AND ic.collection_hash = :collectionHash
        """,
    )
    fun inscriptionItemsFlowByCollectionHash(collectionHash: String): Flow<List<InscriptionItem>>

    @Query(
        """
        SELECT ic.*
        FROM inscription_collections ic
        WHERE  ic.collection_hash = :hash 
        """,
    )
    fun collectionFlowByHash(hash: String): Flow<InscriptionCollection?>

}
