package one.mixin.android.db

import androidx.room3.Dao
import androidx.room3.Query
import one.mixin.android.vo.Hyperlink

@Dao
interface HyperlinkDao : BaseDao<Hyperlink> {
    @Query("SELECT * FROM hyperlinks WHERE hyperlink = :hyperlink")
    fun findHyperlinkByLink(hyperlink: String): Hyperlink?
}
