package one.mixin.android.util.hyperlink

import one.mixin.android.db.HyperlinkDao
import one.mixin.android.db.MessageDao
import one.mixin.android.vo.Hyperlink

fun parseHyperlink(messageId: String, hyperlink: String, hyperlinkDao: HyperlinkDao, messageDao: MessageDao) {
    if (hyperlinkDao.findHyperlinkByLink(hyperlink) == null) {
        try {
            hyperlinkDao.insert(Hyperlink(hyperlink, "", "", "", null))
        } catch (e: Exception) {
        }
    }
}
