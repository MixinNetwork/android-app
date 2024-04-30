package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.InscriptionItem

@Dao
interface InscriptionDao : BaseDao<InscriptionItem> {

    @Query("SELECT i.* FROM inscription_item i LEFT JOIN outputs o ON i.inscription_hash == o.inscription_hash WHERE o.state = 'unspent'")
    fun inscriptions(): LiveData<List<InscriptionItem>>

    @Query("SELECT inscription_hash FROM inscription_item WHERE inscription_hash IN (:hash)")
    suspend fun getExitsHash(hash: List<String>): List<String>
}
