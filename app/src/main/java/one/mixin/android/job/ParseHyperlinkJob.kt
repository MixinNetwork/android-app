package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.vo.Hyperlink
import org.jsoup.Jsoup

class ParseHyperlinkJob(private val hyperlink: String, private val messageId: String) :
    BaseJob(Params(PRIORITY_BACKGROUND).groupBy("parse_hyperlink_group").persist().requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        if (hyperlinkDao.findHyperlinkByLink(hyperlink) == null) {
            val doc = if (hyperlink.startsWith("https://", true) || hyperlink.startsWith("http://", true)) {
                Jsoup.connect(hyperlink).ignoreContentType(true).get()
            } else {
                Jsoup.connect("http://$hyperlink").ignoreContentType(true).get()
            }
            var description: String? = null
            try {
                description = doc.select("meta[name=description]")[0]
                    .attr("content")
            } catch (e: Exception) {
            }

            if (!doc.title().isNullOrBlank() || !description.isNullOrBlank()) {
                hyperlinkDao.insert(Hyperlink(hyperlink, doc.title(), "", description ?: "", null))
            }
        }
        messageDao.updateHyperlink(hyperlink, messageId)
    }

    override fun getRetryLimit(): Int {
        return 1
    }
}
