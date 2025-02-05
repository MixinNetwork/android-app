package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.contants.STICKERS
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerRelationship

@Dao
interface StickerRelationshipDao : BaseDao<StickerRelationship> {
    @Query("SELECT s.* FROM sticker_relationships sr INNER JOIN stickers s ON s.sticker_id = sr.sticker_id WHERE sr.album_id = :id ORDER BY s.created_at DESC")
    fun observeStickersByAlbumId(id: String): LiveData<List<Sticker>>

    @Query("SELECT s.* FROM sticker_relationships sr INNER JOIN stickers s ON s.sticker_id = sr.sticker_id INNER JOIN sticker_albums sa ON sa.album_id = sr.album_id WHERE sr.album_id = :id AND sa.category = 'SYSTEM' ORDER BY s.created_at DESC")
    fun observeSystemStickersByAlbumId(id: String): LiveData<List<Sticker>>

    @Query("SELECT s.* FROM sticker_relationships sr INNER JOIN stickers s ON s.sticker_id = sr.sticker_id WHERE sr.album_id = :id ORDER BY s.created_at DESC")
    suspend fun findStickersByAlbumId(id: String): List<Sticker>

    @Query("SELECT sa.album_id FROM sticker_relationships sr INNER JOIN sticker_albums sa ON sr.album_id = sa.album_id WHERE sr.sticker_id = :stickerId AND sa.category = 'SYSTEM'")
    suspend fun findStickerSystemAlbumId(stickerId: String): String?

    @Query(
        """
        SELECT s.* FROM sticker_albums sa
        INNER JOIN sticker_relationships sr ON sr.album_id = sa.album_id
        INNER JOIN stickers s ON sr.sticker_id = s.sticker_id
        WHERE sa.category = 'PERSONAL' ORDER BY s.created_at
        """,
    )
    fun observePersonalStickers(): LiveData<List<Sticker>>

    @Query("DELETE FROM sticker_relationships  WHERE sticker_id = :stickerId AND album_id = :albumId")
    fun deleteByStickerId(
        stickerId: String,
        albumId: String,
    )

    @Query("SELECT album_id FROM sticker_albums WHERE category = 'PERSONAL'")
    fun getPersonalAlbumId(): String?

    @Query(
        """
        UPDATE messages SET sticker_id = (
            SELECT s.sticker_id FROM stickers s 
            INNER JOIN sticker_relationships sa ON sa.sticker_id = s.sticker_id 
            INNER JOIN sticker_albums a ON a.album_id = sa.album_id
            WHERE a.album_id = messages.album_id AND s.name = messages.name) 
        WHERE category IN ($STICKERS) AND sticker_id IS NULL
        """,
    )
    fun updateMessageStickerId()
}
