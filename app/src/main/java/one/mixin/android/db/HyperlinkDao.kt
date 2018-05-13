package one.mixin.android.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import one.mixin.android.vo.Hyperlink

@Dao
interface HyperlinkDao : BaseDao<Hyperlink> {
    @Query("SELECT * FROM hyperlinks WHERE hyperlink = :hyperlink")
    fun findHyperlinkByLink(hyperlink: String): Hyperlink?
}