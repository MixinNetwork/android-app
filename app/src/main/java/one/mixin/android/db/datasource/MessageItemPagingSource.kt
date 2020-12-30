package one.mixin.android.db.datasource

import android.annotation.SuppressLint
import androidx.paging.PagingSource
import androidx.room.InvalidationTracker
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.vo.MessageItem
import timber.log.Timber
import java.lang.Exception

@SuppressLint("RestrictedApi")
class MessageItemPagingSource(
    val conversationId: String,
    val messageDao: MessageDao,
    val db: MixinDatabase
) : PagingSource<Int, MessageItem>() {
    init {
        try {
            val mObserver = object : InvalidationTracker.Observer(
                "messages",
                "users",
                "snapshots",
                "assets",
                "stickers",
                "hyperlinks",
                "conversations",
                "message_mentions"
            ) {
                override fun onInvalidated(tables: Set<String>) {
                    invalidate()
                }
            }
            db.invalidationTracker.addObserver(mObserver)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MessageItem> {
        val offset = params.key ?: 0

        return LoadResult.Page(
            data = messageDao.messages(conversationId, 30, offset),
            nextKey = offset + 30,
            prevKey = if (offset > 30) {
                offset - 30
            } else {
                null
            }
        )
    }
}