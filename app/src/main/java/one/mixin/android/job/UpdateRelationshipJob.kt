package one.mixin.android.job

import android.annotation.SuppressLint
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipAction.ADD
import one.mixin.android.api.request.RelationshipAction.BLOCK
import one.mixin.android.api.request.RelationshipAction.REMOVE
import one.mixin.android.api.request.RelationshipAction.UNBLOCK
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.session.Session
import one.mixin.android.vo.User
import one.mixin.android.vo.UserRelationship
import one.mixin.android.vo.generateConversationId

class UpdateRelationshipJob(
    private val request: RelationshipRequest,
    private val report: Boolean = false,
) :
    BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).groupBy("relationship").requireNetwork()) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "UpdateRelationshipJob"
    }

    override fun onAdded() {
        if (request.user_id == Session.getAccountId()) {
            return
        }
        when {
            RelationshipAction.valueOf(request.action) == ADD ->
                userDao().updateUserRelationship(request.user_id, UserRelationship.FRIEND.name)
            RelationshipAction.valueOf(request.action) == REMOVE ->
                userDao().updateUserRelationship(request.user_id, UserRelationship.STRANGER.name)
            RelationshipAction.valueOf(request.action) == BLOCK ->
                userDao().updateUserRelationship(request.user_id, UserRelationship.BLOCKING.name)
            RelationshipAction.valueOf(request.action) == UNBLOCK ->
                userDao().updateUserRelationship(request.user_id, UserRelationship.STRANGER.name)
        }
    }

    @SuppressLint("CheckResult")
    override fun onRun() {
        runBlocking {
            if (request.user_id == Session.getAccountId()) {
                return@runBlocking
            }
            handleMixinResponse(
                invokeNetwork = {
                    if (report) {
                        userService.report(request)
                    } else {
                        userService.relationship(request)
                    }
                },
                successBlock = { r ->
                    r.data?.let { u ->
                        updateUser(u)
                        val selfId = Session.getAccountId() ?: return@let
                        val currentConversationId = MixinApplication.conversationId ?: return@let
                        val conversationId = generateConversationId(selfId, u.userId)
                        if (conversationId == currentConversationId) {
                            MessageFlow.updateRelationship(conversationId)
                        }
                    }
                },
            )
        }
    }

    private fun updateUser(u: User) {
        if (u.app != null) {
            u.appId = u.app!!.appId
            appDao().insert(u.app!!)
        }
        userDao().upsert(u)
    }
}
