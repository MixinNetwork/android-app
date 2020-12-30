package one.mixin.android.db.datasource

import android.annotation.SuppressLint
import androidx.paging.PagingSource
import androidx.room.InvalidationTracker
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.vo.ConversationItem
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

@SuppressLint("RestrictedApi")
class ConversationItemPagingSource @Inject constructor(
    val conversationDao: ConversationDao,
    val db: MixinDatabase,
    val circleId: String? = null
) : PagingSource<Int, ConversationItem>() {
    init {
        try {
            val mObserver = object : InvalidationTracker.Observer(
                "message_mentions",
                "conversations",
                "users",
                "messages",
                "snapshots"
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

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ConversationItem> {
        val offset = params.key ?: 0

        return LoadResult.Page(
            data = if (circleId == null) {
                conversationDao.conversationList(offset, 30)
            } else {
                conversationDao.conversationListByCircleId(circleId, offset, 30)
            },
            nextKey = offset + 30,
            prevKey = if (offset > 30) {
                offset - 30
            } else {
                null
            }
        )
    }
}