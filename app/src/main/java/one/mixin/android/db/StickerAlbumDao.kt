package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import one.mixin.android.vo.StickerAlbum

@Dao
interface StickerAlbumDao : BaseDao<StickerAlbum> {

    @Query("SELECT * FROM sticker_albums ORDER BY created_at ASC")
    fun albums(): LiveData<List<StickerAlbum>>
}