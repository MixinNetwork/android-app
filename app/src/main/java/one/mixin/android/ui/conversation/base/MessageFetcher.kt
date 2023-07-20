import androidx.room.RoomDatabase
import one.mixin.android.ui.conversation.base.CompressedList
import one.mixin.android.ui.conversation.base.DataFetcher
import one.mixin.android.vo.MessageItem

class MessageFetcher(val initId:String,db:RoomDatabase):DataFetcher<MessageItem>(db){
    override fun initData(): CompressedList<MessageItem> {
        TODO("Not yet implemented")
    }

    override fun loadRange(): List<MessageItem> {
        TODO("Not yet implemented")
    }
}