package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantAction
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.event.ConversationEvent
import one.mixin.android.event.GroupEvent
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.session.Session
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.ConversationBuilder
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import timber.log.Timber
import java.util.UUID

class ConversationJob(
    private val request: ConversationRequest? = null,
    private val conversationId: String? = null,
    private val participantRequests: List<ParticipantRequest>? = null,
    private val type: Int,
    private val recipientId: String? = null,
) : MixinJob(Params(PRIORITY_UI_HIGH).groupBy(GROUP), UUID.randomUUID().toString()) {
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
        const val TYPE_DISMISS_ADMIN = 8

        const val CREATE_TIMEOUT_MILLIS = 10000L
    }

    private var createCheckRunJob: Job? = null

    override fun onAdded() {
        super.onAdded()
        if (type == TYPE_CREATE) {
            if (!MixinApplication.appContext.networkConnected()) {
                updateConversationStatusFailure()
                return
            }
            createCheckRunJob =
                applicationScope.launch(Dispatchers.IO) {
                    delay(CREATE_TIMEOUT_MILLIS)
                    updateConversationStatusFailure()
                }
        }
    }

    override fun onRun() {
        createCheckRunJob?.cancel()
        createGroup()
    }

    private fun updateConversationStatusFailure() {
        request?.conversationId?.let {
            conversationDao().updateConversationStatusById(it, ConversationStatus.FAILURE.ordinal)
        }
    }

    private fun createGroup() {
        try {
            val response =
                when (type) {
                    TYPE_CREATE ->
                        conversationApi.create(request!!).execute().body()
                    TYPE_ADD ->
                        conversationApi.participants(
                            conversationId!!,
                            ParticipantAction.ADD.name,
                            participantRequests!!,
                        )
                            .execute().body()
                    TYPE_REMOVE ->
                        conversationApi.participants(
                            conversationId!!,
                            ParticipantAction.REMOVE.name,
                            participantRequests!!,
                        )
                            .execute().body()
                    TYPE_UPDATE ->
                        conversationApi.update(conversationId!!, request!!).execute().body()
                    TYPE_MAKE_ADMIN ->
                        conversationApi.participants(
                            conversationId!!,
                            ParticipantAction.ROLE.name,
                            participantRequests!!,
                        )
                            .execute().body()
                    TYPE_EXIT ->
                        conversationApi.exit(conversationId!!).execute().body()
                    TYPE_MUTE ->
                        conversationApi.mute(request!!.conversationId, request).execute().body()
                    TYPE_DISMISS_ADMIN ->
                        conversationApi.participants(
                            conversationId!!,
                            ParticipantAction.ROLE.name,
                            participantRequests!!,
                        )
                            .execute().body()
                    else -> null
                }
            handleResult(response)
        } catch (e: Exception) {
            if (type == TYPE_CREATE) {
                updateConversationStatusFailure()
            } else if (type != TYPE_CREATE || type != TYPE_MUTE) {
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
               insertOrUpdateConversation(cr)
                val participants = mutableListOf<Participant>()
                cr.participants.mapTo(participants) {
                    Participant(cr.conversationId, it.userId, it.role, cr.createdAt)
                }
                participantDao().insertList(participants)
                cr.participantSessions?.let {
                    jobSenderKey.syncParticipantSession(cr.conversationId, it)
                }
                jobManager.addJobInBackground(GenerateAvatarJob(cr.conversationId))
            } else if (type == TYPE_MUTE) {
                if (cr.category == ConversationCategory.CONTACT.name) {
                    recipientId?.let { userDao().updateMuteUntil(it, cr.muteUntil) }
                } else {
                    conversationId?.let { conversationDao().updateGroupMuteUntil(it, cr.muteUntil) }
                }
            } else {
                RxBus.publish(ConversationEvent(type, true))
            }
        } else {
            if (type != TYPE_CREATE || type != TYPE_MUTE) {
                RxBus.publish(ConversationEvent(type, false))
            }
            if (type == TYPE_CREATE) {
                request?.let {
                    conversationDao().updateConversationStatusById(
                        request.conversationId,
                        ConversationStatus.FAILURE.ordinal,
                    )
                }
            }
            if (r?.isSuccess == false) {
                ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
            }
        }
    }

    override fun cancel() {
    }

    fun insertOrUpdateConversation(data: ConversationResponse) {
        var ownerId: String = data.creatorId
        if (data.category == ConversationCategory.CONTACT.name) {
            ownerId = data.participants.find { it.userId != Session.getAccountId() }!!.userId
        }
        var c = conversationDao().findConversationById(data.conversationId)
        if (c == null) {
            val builder = ConversationBuilder(data.conversationId, data.createdAt, ConversationStatus.SUCCESS.ordinal)
            c =
                builder.setOwnerId(ownerId)
                    .setCategory(data.category)
                    .setName(data.name)
                    .setIconUrl(data.iconUrl)
                    .setAnnouncement(data.announcement)
                    .setMuteUntil(data.muteUntil)
                    .setCodeUrl(data.codeUrl)
                    .setExpireIn(data.expireIn)
                    .build()
            conversationDao().upsert(c)
            if (!c.announcement.isNullOrBlank()) {
                RxBus.publish(GroupEvent(data.conversationId))
                MixinApplication.appContext.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION).putBoolean(data.conversationId, true)
            }
        } else {
            val status =
                if (data.participants.find { Session.getAccountId() == it.userId } != null) {
                    ConversationStatus.SUCCESS.ordinal
                } else {
                    ConversationStatus.QUIT.ordinal
                }
            conversationDao().updateConversation(
                data.conversationId,
                ownerId,
                data.category,
                data.name,
                data.announcement,
                data.muteUntil,
                data.createdAt,
                data.expireIn,
                status,
            )
            if (data.announcement.isNotBlank() && c.announcement != data.announcement) {
                RxBus.publish(GroupEvent(data.conversationId))
                MixinApplication.appContext.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION).putBoolean(data.conversationId, true)
            }
        }
    }

}
