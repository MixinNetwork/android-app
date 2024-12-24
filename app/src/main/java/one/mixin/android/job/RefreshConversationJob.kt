package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.api.response.ConversationResponse

import one.mixin.android.event.GroupEvent
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.session.Session
import one.mixin.android.vo.ConversationBuilder
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.vo.SYSTEM_USER

class RefreshConversationJob(val conversationId: String, private val skipRefreshCircle: Boolean = false) :
    MixinJob(
        Params(PRIORITY_UI_HIGH).groupBy("refresh_conversation")
            .requireNetwork().persist(),
        conversationId,
    ) {
    override fun cancel() {
    }

    companion object {
        private const val serialVersionUID = 1L
        const val PREFERENCES_CONVERSATION = "preferences_conversation"
    }

    override fun onRun() {
        if (conversationId == SYSTEM_USER || conversationId == Session.getAccountId()) {
            return
        }
        val localData = participantDao().getRealParticipants(conversationId)

        val call = conversationApi.getConversation(conversationId).execute()
        val response = call.body()
        if (response != null && response.isSuccess) {
            response.data?.let { data ->
                insertOrUpdateConversation(data)
                val participants = mutableListOf<Participant>()
                val conversationUserIds = mutableListOf<String>()
                for (p in data.participants) {
                    val item = Participant(conversationId, p.userId, p.role, p.createdAt!!)
                    if (p.role == ParticipantRole.OWNER.name) {
                        participants.add(0, item)
                    } else {
                        participants.add(item)
                    }
                    conversationUserIds.add(p.userId)
                }

                participantDao().replaceAll(data.conversationId, participants)
                data.participantSessions?.let {
                    jobSenderKey().syncParticipantSession(conversationId, it)
                }

                if (conversationUserIds.isNotEmpty()) {
                    jobManager.addJobInBackground(RefreshUserJob(conversationUserIds, conversationId))
                }
                if (participants.size != localData.size || conversationUserIds.isNotEmpty()) {
                    jobManager.addJobInBackground(GenerateAvatarJob(conversationId))
                }
                if (skipRefreshCircle) return@let
                data.circles?.let { circles ->
                    circles.forEach {
                        val circle = circleDao().findCircleById(it.circleId)
                        if (circle == null) {
                            val circleResponse = circleService.getCircle(it.circleId).execute().body()
                            if (circleResponse?.isSuccess == true) {
                                circleResponse.data?.let { item ->
                                    circleDao().insert(item)
                                }
                            }
                        }
                        circleConversationDao().insertUpdate(it)
                    }
                }
            }
        }
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
