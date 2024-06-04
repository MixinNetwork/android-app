package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.InscriptionCollection

@Dao
interface InscriptionCollectionDao : BaseDao<InscriptionCollection> {
    @Query("SELECT collection_hash FROM inscription_collections WHERE collection_hash = :collectionHash")
    suspend fun exits(collectionHash: String): String?

    @Query("SELECT * FROM inscription_collections WHERE collection_hash = :collectionHash")
    suspend fun findInscriptionCollectionByHash(collectionHash: String): InscriptionCollection?
}
