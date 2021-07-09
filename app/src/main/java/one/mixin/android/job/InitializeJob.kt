package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.db.insertUpdate
import one.mixin.android.extension.nowInUtc
import one.mixin.android.session.Session
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.createConversation
import one.mixin.android.vo.generateConversationId

class InitializeJob(val botId: String) :
    BaseJob(Params(PRIORITY_UI_HIGH).groupBy(GROUP_ID).requireWebSocketConnected().persist()) {
    companion object {
        private const val GROUP_ID = "InitializeJob"
    }

    override fun onRun(): Unit = runBlocking {
        handleMixinResponse(
            invokeNetwork = {
                userService.relationship(RelationshipRequest(botId, RelationshipAction.ADD.name, Constants.TEAM_BOT_NAME))
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
                    return@handleMixinResponse
                }
            }
        )
    }
}
