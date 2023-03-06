package one.mixin.android.fts

import android.os.CancellationSignal
import android.util.ArrayMap
import androidx.core.database.getStringOrNull
import androidx.paging.ItemKeyedDataSource
import androidx.sqlite.db.SimpleSQLiteQuery
import one.mixin.android.db.MixinDatabase
import one.mixin.android.vo.SearchMessageDetailItem

class FtsDataSource(
    val ftsDatabase: FtsDatabase,
    val mixinDatabase: MixinDatabase,
    val query: String,
    val conversationId: String,
    val cancellationSignal: CancellationSignal,
) :
    ItemKeyedDataSource<Int, SearchMessageDetailItem>() {

    private var formOldData = false
    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<SearchMessageDetailItem>,
    ) {
        callback.onResult(getData(params.requestedLoadSize,-1,false))
    }

    override fun loadAfter(
        params: LoadParams<Int>,
        callback: LoadCallback<SearchMessageDetailItem>,
    ) {
        callback.onResult(getData(params.requestedLoadSize ,params.key))
    }

    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<SearchMessageDetailItem>,
    ) {
        callback.onResult(getData(params.requestedLoadSize ,params.key, true))
    }

    override fun getKey(item: SearchMessageDetailItem): Int {
        return fastKeyMap.values.indexOf(item.messageId)
    }

    private fun getData(
        size: Int,
        key: Int,
        reverse: Boolean = false,
    ): List<SearchMessageDetailItem> {
        if (reverse) {
            if (key > 1) {
                repeat(key - 1) {
                    val ids = mutableListOf<String>()
                    repeat(size) {
                        fastKeyMap[key-1-it]?.let { it1 -> ids.add(it1) }
                    }
                    return getData(ids.reversed())
                }
            } else {
                return emptyList()
            }
        } else if (fastKeyMap[key + size + 1] != null) {
            val ids = mutableListOf<String>()
            repeat(size) {
                fastKeyMap[key + 1 + it]?.let { it1 -> ids.add(it1) }
            }
            return getData(ids)
        }
        return if (formOldData) {
            getOldData(size, key + 1)
        } else {
            getNewData(size, key + 1)
        }
    }

    private val oldFtsCursor by lazy {
        mixinDatabase.query(SimpleSQLiteQuery("SELECT m.id FROM messages m WHERE m.id IN (SELECT message_id FROM messages_fts4 WHERE messages_fts4 MATCH '$query') AND m.conversation_id = '$conversationId' ORDER BY m.created_at DESC"))
    }

    private fun getOldData(size: Int, startKey: Int): List<SearchMessageDetailItem> {
        val ids = mutableListOf<String>()
        var index = 0
        while (oldFtsCursor.moveToNext() && index < size) {
            val messageId = oldFtsCursor.getStringOrNull(0) ?: continue
            ids.add(messageId)
            index++
        }
        return getData(ids, startKey)
    }


    private val newFtsCursor by lazy {
        ftsDatabase.query(
            SimpleSQLiteQuery(
                "SELECT message_id FROM messages_metas WHERE conversation_id = '$conversationId' AND doc_id IN (SELECT docid FROM messages_fts WHERE content MATCH '$query') ORDER BY created_at DESC",
            ),
            cancellationSignal,
        )
    }

    private fun getNewData(size: Int, startKey: Int): List<SearchMessageDetailItem> {
        val ids = mutableListOf<String>()
        var index = 0
        while (newFtsCursor.moveToNext() && index < size) {
            val messageId = newFtsCursor.getStringOrNull(0) ?: continue
            ids.add(messageId)
            index++
        }
        val result = getData(ids, startKey)
        if (result.size < size) { // If can't get the full-size data, will get it from the old fts.
            formOldData = true
        }
        return result
    }

    private fun getData(ids: List<String>, startKey: Int? = null): List<SearchMessageDetailItem> {
        val result = mixinDatabase.messageDao().getSearchMessageDetailItemsByIds(ids)
        if (startKey != null) {
            result.forEachIndexed { itemIndex, item ->
                fastKeyMap[itemIndex + startKey] = item.messageId
            }
        }
        return result
    }

    private val fastKeyMap = ArrayMap<Int, String>() // Cache id and index
}
