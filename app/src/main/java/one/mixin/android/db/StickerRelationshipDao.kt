package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerRelationship

@Dao
interface StickerRelationshipDao: BaseDao<StickerRelationship> {

    @Query("SELECT s.* FROM sticker_relationships sr INNER JOIN stickers s ON s.sticker_id = sr.sticker_id WHERE sr.album_id = :id ORDER BY s.created_at " +
        "LIMIT 99")
    fun observeStickersByAlbumId(id: String): LiveData<List<Sticker>>

    @Query("SELECT s.* FROM sticker_albums sa " +
        "INNER JOIN sticker_relationships sr ON sr.album_id = sa.album_id " +
        "INNER JOIN stickers s ON sr.sticker_id = s.sticker_id " +
        "WHERE sa.category = 'PERSONAL' ORDER BY s.created_at LIMIT 99")
    fun observePersonalStickers(): LiveData<List<Sticker>>

    @Query("DELETE FROM sticker_relationships WHERE sticker_id = :stickerId")
    fun deleteByStickerId(stickerId: String)
}