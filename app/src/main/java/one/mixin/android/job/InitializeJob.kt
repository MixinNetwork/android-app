package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.db.insertUpdate

class InitializeJob(val botId: String, private val botName: String) :
    BaseJob(Params(PRIORITY_UI_HIGH).groupBy(GROUP_ID).requireWebSocketConnected().persist()) {
    companion object {
        private const val GROUP_ID = "InitializeJob"
    }

    override fun onRun(): Unit = runBlocking {
        handleMixinResponse(
            invokeNetwork = {
                userService.relationship(RelationshipRequest(botId, RelationshipAction.ADD.name, botName))
            },
            successBlock = {
                it.data?.let { u ->
                    userDao.insertUpdate(u, appDao)
                    return@handleMixinResponse
                }
            }
        )
    }
}
