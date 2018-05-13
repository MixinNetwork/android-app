package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import one.mixin.android.vo.Sticker

@Dao
interface StickerDao : BaseDao<Sticker> {

    @Query("SELECT * FROM stickers WHERE album_id = :id")
    fun getStickersByAlbumId(id: String): List<Sticker>

    @Query("SELECT * FROM stickers WHERE last_use_at > 0 ORDER BY last_use_at DESC LIMIT 20")
    fun recentUsedStickers(): LiveData<List<Sticker>>

    @Query("UPDATE stickers SET last_use_at = :at WHERE album_id = :albumId and name = :name")
    fun updateUsedAt(albumId: String, name: String, at: String)

    @Query("SELECT * FROM stickers WHERE album_id = :albumId and name = :name")
    fun getStickerByUnique(albumId: String, name: String): Sticker?
}