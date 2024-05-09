package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.InscriptionCollection
import one.mixin.android.vo.InscriptionItem
import one.mixin.android.vo.safe.SafeInscription

@Dao
interface InscriptionDao : BaseDao<InscriptionItem> {

    @Query("SELECT inscription_hash FROM inscription_items WHERE inscription_hash IN (:hash)")
    suspend fun getExitsHash(hash: List<String>): List<String>

    @Query("SELECT DISTINCT collection_hash FROM inscription_items WHERE inscription_hash IN (:hash)")
    suspend fun getInscriptionCollectionIds(hash: List<String>): List<String>

    @Query("SELECT i.collection_hash, i.inscription_hash, ic.name, i.sequence, i.content_type, i.content_url, ic.icon_url FROM inscription_items i LEFT JOIN inscription_collections ic on ic.collection_hash = i.collection_hash WHERE inscription_hash = :hash AND ic.collection_hash IS NOT NULL")
    fun inscriptionByHash(hash: String): LiveData<SafeInscription?>

    @Query("SELECT * FROM inscription_items WHERE inscription_hash = :hash")
    fun findInscriptionByHash(hash: String): InscriptionItem?

    @Query("SELECT ic.* FROM inscription_items i LEFT JOIN inscription_collections ic on ic.collection_hash = i.collection_hash WHERE i.inscription_hash = :hash")
    fun findInscriptionCollectionByHash(hash: String): InscriptionCollection?
}
