package one.mixin.android.db.datasource

import androidx.paging.PagingSource
import one.mixin.android.db.ConversationDao
import one.mixin.android.vo.ConversationItem
import javax.inject.Inject

class ConversationItemPagingSource @Inject constructor(val conversationDao: ConversationDao) :
    PagingSource<Int, ConversationItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ConversationItem> {
        val offset = params.key ?: 0

        return LoadResult.Page(
            data = conversationDao.conversationList(offset, 30),
            nextKey = offset + 30,
            prevKey = if (offset > 30) {
                offset - 30
            } else {
                null
            }
        )
    }
}