package one.mixin.android.util

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.room.ColumnInfo
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.joinWhiteSpace
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageFts4
import one.mixin.android.vo.isFtsMessage

data class QueryMessage(
    @ColumnInfo(name = "message_id")
    val messageId: String,
    val content: String?,
    val name: String?
)

object MessageFts4Helper {

    private const val SYNC_FTS4_LIMIT = 100

    suspend fun syncMessageFts4(context: Context) {
        val messageDao = MixinDatabase.getDatabase(context).messageDao()
        val messageFts4Dao = MixinDatabase.getDatabase(context).messageFts4Dao()

        // TODO read last read offset form SP
        var offset = 0
        while (true) {
            val queryMessageList = messageDao.batchQueryMessages(SYNC_FTS4_LIMIT, offset)
            val messageFts4List = arrayListOf<MessageFts4>()
            queryMessageList.forEach { item ->
                val name = item.name.joinWhiteSpace()
                val content = item.content.joinWhiteSpace()
                messageFts4List.add(MessageFts4(item.messageId, name + content))
            }
            messageFts4Dao.insertListSuspend(messageFts4List)
            offset += queryMessageList.size
            if (queryMessageList.size < SYNC_FTS4_LIMIT) {
                break
            }
        }
    }

    @WorkerThread
    fun insertOrReplaceMessageFts4(context: Context, message: Message) {
        if (!message.isFtsMessage()) return

        val messageFts4Dao = MixinDatabase.getDatabase(context).messageFts4Dao()
        val name = message.name.joinWhiteSpace()
        val content = message.content.joinWhiteSpace()
        messageFts4Dao.insert(MessageFts4(message.id, name + content))
    }
}
