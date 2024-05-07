package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.InscriptionItem

@Dao
interface InscriptionDao : BaseDao<InscriptionItem> {

    @Query("SELECT inscription_hash FROM inscription_item WHERE inscription_hash IN (:hash)")
    suspend fun getExitsHash(hash: List<String>): List<String>
}
