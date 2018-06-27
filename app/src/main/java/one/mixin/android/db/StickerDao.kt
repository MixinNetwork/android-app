package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import one.mixin.android.vo.Sticker

@Dao
interface StickerDao : BaseDao<Sticker> {

    @Query("SELECT * FROM stickers WHERE last_use_at > 0 ORDER BY last_use_at DESC LIMIT 20")
    fun recentUsedStickers(): LiveData<List<Sticker>>

    @Query("UPDATE stickers SET last_use_at = :at WHERE sticker_id = :stickerId")
    fun updateUsedAt(stickerId: String, at: String)

    @Query("SELECT * FROM stickers WHERE sticker_id = :stickerId")
    fun getStickerByUnique(stickerId: String): Sticker?

    @Query("DELETE FROM stickers WHERE sticker_id = :stickerId")
    fun deleteByStickerId(stickerId: String)
}