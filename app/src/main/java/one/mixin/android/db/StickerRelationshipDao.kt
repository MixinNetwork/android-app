package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerRelationship

@Dao
interface StickerRelationshipDao : BaseDao<StickerRelationship> {

    @Query("SELECT s.* FROM sticker_relationships sr INNER JOIN stickers s ON s.sticker_id = sr.sticker_id WHERE sr.album_id = :id ORDER BY s.created_at DESC")
    fun observeStickersByAlbumId(id: String): LiveData<List<Sticker>>

    @Query(
        """
        SELECT s.* FROM sticker_albums sa
        INNER JOIN sticker_relationships sr ON sr.album_id = sa.album_id
        INNER JOIN stickers s ON sr.sticker_id = s.sticker_id
        WHERE sa.category = 'PERSONAL' ORDER BY s.created_at
    """
    )
    fun observePersonalStickers(): LiveData<List<Sticker>>

    @Query("DELETE FROM sticker_relationships  WHERE sticker_id = :stickerId AND album_id = :albumId")
    fun deleteByStickerId(stickerId: String, albumId: String)

    @Query("SELECT album_id FROM sticker_albums WHERE category = 'PERSONAL'")
    fun getPersonalAlbumId(): String?

    @Query(
        "UPDATE messages SET sticker_id = (" +
            "                SELECT s.sticker_id FROM stickers s " +
            "                    INNER JOIN sticker_relationships sa ON sa.sticker_id = s.sticker_id " +
            "                    INNER JOIN sticker_albums a ON a.album_id = sa.album_id " +
            "                    WHERE a.album_id = messages.album_id AND s.name = messages.name " +
            "            ) WHERE category IN ('SIGNAL_STICKER','PLAIN_STICKER') AND sticker_id IS NULL"
    )
    fun updateMessageStickerId()
}
