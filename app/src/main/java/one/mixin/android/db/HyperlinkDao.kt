package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Hyperlink

@Dao
interface HyperlinkDao : BaseDao<Hyperlink> {
    @Query("SELECT * FROM hyperlinks WHERE hyperlink = :hyperlink")
    fun findHyperlinkByLink(hyperlink: String): Hyperlink?
}