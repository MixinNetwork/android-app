package one.mixin.android.job

import androidx.work.WorkManager
import androidx.work.workDataOf
import com.birbit.android.jobqueue.Params
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantAction
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.db.insertConversation
import one.mixin.android.event.ConversationEvent
import one.mixin.android.extension.enqueueOneTimeRequest
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.ConversationBuilder
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.worker.GenerateAvatarWorker
import timber.log.Timber
import java.util.UUID

class ConversationJob(
    private val request: ConversationRequest? = null,
    private val conversationId: String? = null,
    private val participantRequests: List<ParticipantRequest>? = null,
    private val type: Int,
    private val recipientId: String? = null
) : MixinJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).groupBy(GROUP), UUID.randomUUID().toString()) {

    companion object {
        const val GROUP = "ConversationJob"
        private const val serialVersionUID = 1L

        const val TYPE_CREATE = 0
        const val TYPE_ADD = 1
        const val TYPE_REMOVE = 2
        const val TYPE_UPDATE = 3
        const val TYPE_MAKE_ADMIN = 4
        const val TYPE_EXIT = 5
        const val TYPE_DELETE = 6
        const val TYPE_MUTE = 7
    }

    override fun onRun() {
        createGroup()
    }

    private fun createGroup() {
        try {
            val response = when (type) {
                TYPE_CREATE ->
                    conversationApi.create(request!!).execute().body()
                TYPE_ADD ->
                    conversationApi.participants(conversationId!!, ParticipantAction.ADD.name, participantRequests!!)
                        .execute().body()
                TYPE_REMOVE ->
                    conversationApi.participants(conversationId!!, ParticipantAction.REMOVE.name, participantRequests!!)
                        .execute().body()
                TYPE_UPDATE ->
                    conversationApi.update(conversationId!!, request!!).execute().body()
                TYPE_MAKE_ADMIN ->
                    conversationApi.participants(conversationId!!, ParticipantAction.ROLE.name, participantRequests!!)
                        .execute().body()
                TYPE_EXIT ->
                    conversationApi.exit(conversationId!!).execute().body()
                TYPE_MUTE ->
                    conversationApi.mute(request!!.conversationId, request).execute().body()
                else -> null
            }
            handleResult(response)
        } catch (e: Exception) {
            if (type != TYPE_CREATE || type != TYPE_MUTE) {
                RxBus.publish(ConversationEvent(type, false))
                ErrorHandler.handleError(e)
            }
            Timber.e(e)
        }
    }

    private fun handleResult(r: MixinResponse<ConversationResponse>?) {
        if (r != null && r.isSuccess && r.data != null) {
            val cr = r.data!!
            if (type == TYPE_CREATE) {
                val conversation = ConversationBuilder(cr.conversationId,
                    cr.createdAt, ConversationStatus.SUCCESS.ordinal)
                    .setOwnerId(cr.creatorId)
                    .setName(cr.name)
                    .setCategory(cr.category)
                    .setAnnouncement(cr.announcement)
                    .setUnseenMessageCount(0)
                    .setIconUrl(cr.iconUrl)
                    .setCodeUrl(cr.codeUrl)
                    .build()
                conversationDao.insertConversation(conversation)

                val participants = mutableListOf<Participant>()
                cr.participants.mapTo(participants) { Participant(cr.conversationId, it.userId, it.role, cr.createdAt) }
                participantDao.insertList(participants)
                WorkManager.getInstance().enqueueOneTimeRequest<GenerateAvatarWorker>(
                    workDataOf(GenerateAvatarWorker.GROUP_ID to cr.conversationId))
            } else if (type == TYPE_MUTE) {
                if (cr.category == ConversationCategory.CONTACT.name) {
                    recipientId?.let { userDao.updateDuration(it, cr.muteUntil) }
                } else {
                    conversationId?.let { conversationDao.updateGroupDuration(it, cr.muteUntil) }
                }
            } else {
                RxBus.publish(ConversationEvent(type, true))
            }
        } else {
            if (type != TYPE_CREATE || type != TYPE_MUTE) {
                RxBus.publish(ConversationEvent(type, false))
            } else if (type == TYPE_CREATE) {
                request?.let {
                    conversationDao.updateConversationStatusById(request.conversationId,
                        ConversationStatus.FAILURE.ordinal)
                }
            }
            if (r?.isSuccess == false) {
                ErrorHandler.handleMixinError(r.errorCode)
            }
        }
    }

    override fun cancel() {
    }
}