package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.db.insertUpdate
import one.mixin.android.extension.nowInUtc
import one.mixin.android.session.Session
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.createConversation
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.generateConversationId
import java.util.UUID

class InitializeJob(val botId: String) :
    BaseJob(Params(PRIORITY_UI_HIGH).groupBy(GROUP_ID).requireWebSocketConnected().persist()) {
    companion object {
        private const val GROUP_ID = "InitializeJob"
    }

    override fun onRun(): Unit = runBlocking {
        handleMixinResponse(
            invokeNetwork = {
                userService.relationship(RelationshipRequest(botId, RelationshipAction.ADD.name))
            },
            successBlock = {
                it.data?.let { u ->
                    userDao.insertUpdate(u, appDao)
                    val conversationId = generateConversationId(Session.getAccountId()!!, botId)
                    val createdAt = nowInUtc()
                    val conversation = createConversation(
                        conversationId,
                        ConversationCategory.CONTACT.name,
                        botId,
                        ConversationStatus.START.ordinal
                    )
                    val participants = arrayListOf(
                        Participant(conversationId, Session.getAccountId()!!, "", createdAt),
                        Participant(conversationId, botId, "", createdAt)
                    )
                    conversationDao.insert(conversation)
                    participantDao.insertList(participants)
                    val message = createMessage(
                        UUID.randomUUID().toString(),
                        conversationId,
                        Session.getAccountId()!!,
                        MessageCategory.PLAIN_TEXT.name,
                        MixinApplication.get().getString(R.string.hi),
                        nowInUtc(),
                        MessageStatus.SENDING.name
                    )
                    jobManager.addJobInBackground(SendMessageJob(message))
                    return@handleMixinResponse
                }
            }
        )
    }
}
