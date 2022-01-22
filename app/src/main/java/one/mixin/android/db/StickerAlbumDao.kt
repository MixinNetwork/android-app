package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerAlbumAdded
import one.mixin.android.vo.StickerAlbumOrder

@Dao
interface StickerAlbumDao : BaseDao<StickerAlbum> {

    @Query("SELECT * FROM sticker_albums WHERE category = 'SYSTEM' AND added = 1 ORDER BY ordered_at DESC, created_at DESC")
    fun observeSystemAddedAlbums(): LiveData<List<StickerAlbum>>

    @Query("SELECT * FROM sticker_albums WHERE category = 'SYSTEM' ORDER BY created_at DESC")
    fun observeSystemAlbums(): LiveData<List<StickerAlbum>>

    @Query("SELECT * FROM sticker_albums WHERE category = 'PERSONAL' ORDER BY created_at ASC")
    suspend fun getPersonalAlbums(): StickerAlbum?

    @Query("SELECT * FROM sticker_albums WHERE album_id = :albumId")
    suspend fun findAlbumById(albumId: String): StickerAlbum?

    @Update(entity = StickerAlbum::class)
    suspend fun updateOrderedAt(order: StickerAlbumOrder)

    @Update(entity = StickerAlbum::class)
    suspend fun updateAdded(added: StickerAlbumAdded)

    @Query("SELECT * FROM sticker_albums WHERE album_id = :albumId")
    fun observeAlbumById(albumId: String): LiveData<StickerAlbum>

    @Query("SELECT * FROM sticker_albums WHERE album_id = :albumId AND category = 'SYSTEM'")
    fun observeSystemAlbumById(albumId: String): LiveData<StickerAlbum>

    @Query("SELECT created_at FROM sticker_albums ORDER BY created_at DESC LIMIT 1")
    suspend fun findLatestCreatedAt(): String?

    @Query("SELECT max(ordered_at) FROM sticker_albums")
    suspend fun findMaxOrder(): Int?
}
