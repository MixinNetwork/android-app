package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import one.mixin.android.vo.Sticker

@Dao
interface StickerDao : BaseDao<Sticker> {
    @Transaction
    fun insertUpdate(s: Sticker) {
        val sticker = getStickerByUnique(s.stickerId)
        if (sticker != null) {
            s.lastUseAt = sticker.lastUseAt
        }
        if (s.createdAt == "") {
            s.createdAt = System.currentTimeMillis().toString()
        }
        insert(s)
    }


    @Query("SELECT * FROM stickers WHERE last_use_at > 0 ORDER BY last_use_at DESC LIMIT 20")
    fun recentUsedStickers(): LiveData<List<Sticker>>

    @Query("UPDATE stickers SET last_use_at = :at WHERE sticker_id = :stickerId")
    suspend fun updateUsedAt(
        stickerId: String,
        at: String,
    )

    @Query("SELECT * FROM stickers WHERE sticker_id = :stickerId")
    fun getStickerByUnique(stickerId: String): Sticker?

    @Query("DELETE FROM stickers WHERE sticker_id = :stickerId")
    fun deleteByStickerId(stickerId: String)

    @Query("SELECT s.* FROM sticker_relationships sr, stickers s WHERE sr.sticker_id = s.sticker_id AND sr.album_id = :id AND s.name = :name")
    fun getStickerByAlbumIdAndName(
        id: String,
        name: String,
    ): Sticker?

    @Query("SELECT * FROM stickers WHERE sticker_id = :stickerId")
    suspend fun findStickerById(stickerId: String): Sticker?

    @Query("SELECT * FROM stickers WHERE sticker_id = :stickerId")
    fun observeStickerById(stickerId: String): LiveData<Sticker>

    @Query("UPDATE stickers SET album_id = :albumId WHERE sticker_id = :stickerId")
    suspend fun updateAlbumId(
        stickerId: String,
        albumId: String,
    )

    @Query("SELECT s.* FROM stickers s WHERE s.rowid > :rowId ORDER BY s.rowid ASC LIMIT :limit")
    fun getStickersByLimitAndRowId(
        limit: Int,
        rowId: Long,
    ): List<Sticker>

    @Query("SELECT rowid FROM stickers WHERE sticker_id = :stickerId")
    fun getStickerRowId(stickerId: String): Long?

    @Query("SELECT count(1) FROM stickers")
    fun countStickers(): Long

    @Query("SELECT count(1) FROM stickers WHERE rowid > :rowId")
    fun countStickers(rowId: Long): Long
}
