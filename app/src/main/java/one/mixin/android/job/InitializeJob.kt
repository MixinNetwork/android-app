package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest

class InitializeJob(private val botId: String) :
    BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).requireNetwork().persist()) {
    companion object {
        private var serialVersionUID: Long = 2L
        private const val GROUP_ID = "InitializeJob"
    }

    override fun onRun(): Unit =
        runBlocking {
            if (botId.isEmpty()) {
                return@runBlocking
            }
            updateRelationship(botId)
        }

    private suspend fun updateRelationship(botId: String) {
        handleMixinResponse(
            invokeNetwork = {
                userService.relationship(RelationshipRequest(botId, RelationshipAction.ADD.name))
            },
            defaultErrorHandle = {},
            defaultExceptionHandle = {},
            successBlock = {
                it.data?.let { u ->
                    userDao.insertUpdate(u, appDao)
                    return@handleMixinResponse
                }
            },
        )
    }
}
