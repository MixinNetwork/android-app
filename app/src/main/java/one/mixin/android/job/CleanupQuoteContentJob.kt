package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.util.GsonHelper
import timber.log.Timber

class CleanupQuoteContentJob(private val rowId: Long) : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "CleanupQuoteContentJob"
    }

    override fun onRun() =
        runBlocking {
            val gson = GsonHelper.customGson
            val list = messageDao().findBigQuoteMessage(rowId, 100)
            list.forEach { quoteMinimal ->
                val quoteMsg = messageDao().findQuoteMessageItemById(quoteMinimal.conversationId, quoteMinimal.quoteMessageId)
                if (quoteMsg == null) {
                    messageDao().updateQuoteContentNullByQuoteMessageId(quoteMinimal.conversationId, quoteMinimal.quoteMessageId)
                } else {
                    quoteMsg.thumbImage =
                        if ((quoteMsg.thumbImage?.length ?: 0) > Constants.MAX_THUMB_IMAGE_LENGTH) {
                            Constants.DEFAULT_THUMB_IMAGE
                        } else {
                            quoteMsg.thumbImage
                        }
                    messageDao().updateQuoteContentByQuoteId(quoteMinimal.conversationId, quoteMinimal.quoteMessageId, gson.toJson(quoteMsg))
                }
            }
            Timber.e("process ${list.size}")
            if (list.size < 100) {
                PropertyHelper.updateKeyValue(Constants.Account.PREF_CLEANUP_QUOTE_CONTENT, false)
            } else {
                jobManager.addJobInBackground(CleanupQuoteContentJob(list.last().rowId))
            }
        }
}
