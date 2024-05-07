package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.InscriptionItem

@Dao
interface InscriptionDao : BaseDao<InscriptionItem> {

    @Query("SELECT inscription_hash FROM inscription_items WHERE inscription_hash IN (:hash)")
    suspend fun getExitsHash(hash: List<String>): List<String>

    @Query("SELECT DISTINCT collection_hash FROM inscription_items WHERE inscription_hash IN (:hash)")
    suspend fun getInscriptionCollectionIds(hash: List<String>): List<String>

    @Query("SELECT * FROM inscription_items WHERE inscription_hash = :hash")
    fun inscriptionByHash(hash: String): LiveData<InscriptionItem?>
}
