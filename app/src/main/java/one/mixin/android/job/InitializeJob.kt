package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.db.insertUpdate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putBoolean
import one.mixin.android.session.Session
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.generateConversationId
import java.util.UUID

class InitializeJob :
    BaseJob(Params(PRIORITY_UI_HIGH).groupBy(GROUP_ID).requireWebSocketConnected().persist()) {
    companion object {
        private const val GROUP_ID = "InitializeJob"
    }

    override fun onAdded() {
        super.onAdded()
        MixinApplication.get().defaultSharedPreferences.putBoolean(
            Constants.Account.PREF_INITIALIZE,
            true
        )
    }

    override fun onRun() = runBlocking {
        val botId = MixinApplication.get().getString(R.string.initializeBotId)
        handleMixinResponse(
            invokeNetwork = {
                userService.relationship(RelationshipRequest(botId, RelationshipAction.ADD.name))
            },
            successBlock = {
                it.data?.let { u ->
                    userDao.insertUpdate(u, appDao)
                    return@handleMixinResponse u
                }
            }
        )
        val message = createMessage(
            UUID.randomUUID().toString(),
            generateConversationId(Session.getAccountId()!!, botId),
            Session.getAccountId()!!,
            MessageCategory.PLAIN_TEXT.name,
            MixinApplication.get().getString(R.string.hi),
            nowInUtc(),
            MessageStatus.SENDING.name
        )
        jobManager.addJobInBackground(SendMessageJob(message))
        return@runBlocking
    }
}
