package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.db.batchMarkReadAndTake
import one.mixin.android.session.Session
import one.mixin.android.vo.MessageMinimal
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createAckJob
import one.mixin.android.websocket.ACKNOWLEDGE_MESSAGE_RECEIPTS
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.CREATE_MESSAGE

class MarkReadJob(val conversationId: String, val accountId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH).setGroupId(conversationId).addTags(GROUP).persist()
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "MarkReadJob"
    }

    override fun onRun() = runBlocking {
        val lastMessageId = messageDao.findLastMessageId(conversationId) ?: return@runBlocking
        while (true) {
            val list = messageDao.getUnreadMessage(conversationId, accountId, lastMessageId, Constants.MARK_LIMIT)
            if (list.isEmpty()) return@runBlocking
            messageDao.batchMarkReadAndTake(
                conversationId,
                accountId,
                list.last().rowId
            )
            list.map {
                createAckJob(
                    ACKNOWLEDGE_MESSAGE_RECEIPTS,
                    BlazeAckMessage(it.id, MessageStatus.READ.name)
                )
            }.let {
                jobDao.insertList(it)
            }
            createReadSessionMessage(list, conversationId)
            if (list.size < Constants.MARK_LIMIT) {
                return@runBlocking
            }
        }
    }

    private fun createReadSessionMessage(list: List<MessageMinimal>, conversationId: String) {
        Session.getExtensionSessionId()?.let {
            list.map {
                createAckJob(
                    CREATE_MESSAGE,
                    BlazeAckMessage(it.id, MessageStatus.READ.name),
                    conversationId
                )
            }.let {
                jobDao.insertList(it)
            }
        }
    }
}
