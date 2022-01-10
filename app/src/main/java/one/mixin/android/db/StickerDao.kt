package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Sticker

@Dao
interface StickerDao : BaseDao<Sticker> {

    @Query("SELECT * FROM stickers WHERE last_use_at > 0 ORDER BY last_use_at DESC LIMIT 20")
    fun recentUsedStickers(): LiveData<List<Sticker>>

    @Query("UPDATE stickers SET last_use_at = :at WHERE sticker_id = :stickerId")
    suspend fun updateUsedAt(stickerId: String, at: String)

    @Query("SELECT * FROM stickers WHERE sticker_id = :stickerId")
    fun getStickerByUnique(stickerId: String): Sticker?

    @Query("DELETE FROM stickers WHERE sticker_id = :stickerId")
    fun deleteByStickerId(stickerId: String)

    @Query("SELECT s.* FROM sticker_relationships sr, stickers s WHERE sr.sticker_id = s.sticker_id AND sr.album_id = :id AND s.name = :name")
    fun getStickerByAlbumIdAndName(id: String, name: String): Sticker?

    @Query("SELECT * FROM stickers WHERE sticker_id = :stickerId")
    suspend fun findStickerById(stickerId: String): Sticker?
}
