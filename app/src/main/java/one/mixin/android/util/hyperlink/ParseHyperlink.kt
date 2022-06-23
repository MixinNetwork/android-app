package one.mixin.android.util.hyperlink

import one.mixin.android.db.HyperlinkDao
import one.mixin.android.vo.Hyperlink

fun parseHyperlink(hyperlink: String, hyperlinkDao: HyperlinkDao) {
    if (hyperlinkDao.findHyperlinkByLink(hyperlink) == null) {
        try {
            hyperlinkDao.insert(Hyperlink(hyperlink, "", "", "", null))
        } catch (_: Exception) {
        }
    }
}
