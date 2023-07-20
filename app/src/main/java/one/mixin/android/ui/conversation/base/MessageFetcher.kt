import androidx.room.RoomDatabase
import one.mixin.android.ui.conversation.base.CompressedList
import one.mixin.android.ui.conversation.base.DataFetcher
import one.mixin.android.vo.MessageItem

class MessageFetcher(db: RoomDatabase, val initId: String? = null) : DataFetcher<MessageItem>(db) {
    override fun initData(): CompressedList<MessageItem> {
        TODO("Not yet implemented")
    }

    override fun loadRange(): List<MessageItem> {
        TODO("Not yet implemented")
    }

    override fun loadData(position: Int, callback: (List<MessageItem>) -> Unit) {
        TODO("Not yet implemented")
    }
}
