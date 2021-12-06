package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.StickerAlbum

@Dao
interface StickerAlbumDao : BaseDao<StickerAlbum> {

    @Query("SELECT * FROM sticker_albums WHERE category = 'SYSTEM' ORDER BY created_at DESC")
    fun getSystemAlbums(): LiveData<List<StickerAlbum>>

    @Query("SELECT * FROM sticker_albums WHERE category = 'PERSONAL' ORDER BY created_at ASC")
    suspend fun getPersonalAlbums(): StickerAlbum?

    @Query("SELECT * FROM sticker_albums WHERE album_id = :albumId")
    suspend fun findAlbumById(albumId: String): StickerAlbum?
}
